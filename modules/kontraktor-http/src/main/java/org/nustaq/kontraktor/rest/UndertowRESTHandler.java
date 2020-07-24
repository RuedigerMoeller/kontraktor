package org.nustaq.kontraktor.rest;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.Pair;
import org.nustaq.serialization.FSTConfiguration;
import org.xnio.channels.StreamSourceChannel;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

public class UndertowRESTHandler implements HttpHandler {

    protected static final Object NOVAL = new Object();
    protected String basePath;
    protected Actor facade;
    protected FSTConfiguration jsonConf = FSTConfiguration.createJsonConfiguration();
    protected Function<HeaderMap,IPromise> requestAuthenticator;
    protected Set<String> allowedMethods;
    protected Consumer<HttpServerExchange> prepareResponse;

    public UndertowRESTHandler(
        String basePath,
        Actor facade,
        Function<HeaderMap,IPromise> requestAuthenticator,
        Consumer<HttpServerExchange> prepareResponse
    ) {
        this.basePath = basePath;
        this.facade = facade;
        this.requestAuthenticator = requestAuthenticator;
        allowedMethods = new HashSet<>();
        Arrays.stream(new String[] {
            "get","put","patch","post","delete","head","option"
        }).forEach( s -> allowedMethods.add(s) );
        this.prepareResponse = prepareResponse;
    }

    public void setAllowedMethods(Set<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if ( requestAuthenticator != null ) {
            exchange.dispatch();
            requestAuthenticator.apply(exchange.getRequestHeaders()).then( (r,e) -> {
                if ( e != null ) {
                    exchange.setResponseCode(403);
                    exchange.getResponseSender().send(""+e);
                } else {
                    handleInternal(exchange,r);
                }
            });
        } else {
            handleInternal(exchange, null);
        }
    }

    private void handleInternal(HttpServerExchange exchange, Object credentials) {
        if ( prepareResponse != null ) {
            prepareResponse.accept(exchange);
        }
        String requestPath = exchange.getRequestPath();
        requestPath = requestPath.substring(basePath.length());
        while ( requestPath.startsWith("/") ) {
            requestPath = requestPath.substring(1);
        }
        String[] split = requestPath.split("/");
        String method = ""+exchange.getRequestMethod();
        String rawMethodName = method.toLowerCase();
        String methodName = rawMethodName;
        if ( ! allowedMethods.contains(methodName) ) {
            exchange.setResponseCode(400);
            exchange.endExchange();
            return;
        }
        if (split.length > 0 && split[0].length() > 1 ) {
            methodName += split[0].substring(0,1).toUpperCase()+split[0].substring(1);
        }
        Method m = facade.getActor().__getCachedMethod(methodName,facade,null);
        if ( m == null ) {
            m = facade.getActor().__getCachedMethod(rawMethodName,facade,null);
            if ( m == null ) {
                exchange.setResponseCode(404);
                exchange.endExchange();
                return;
            }
        }

        ContentType ct = m.getAnnotation(ContentType.class);
        if ( ct != null ) {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, ct.value());
        }
        // read post data.
        String first = exchange.getRequestHeaders().getFirst(Headers.CONTENT_LENGTH);
        if ( first != null ) {
            int len = Integer.parseInt(first);
            StreamSourceChannel requestChannel = exchange.getRequestChannel();
            ByteBuffer buf = ByteBuffer.allocate(len);
            Method finalM = m;
            checkExchangeState(exchange);
            AtomicBoolean hadResponse = new AtomicBoolean(false);
            requestChannel.getReadSetter().set(streamSourceChannel -> {
                checkExchangeState(exchange);
                try {
                        streamSourceChannel.read(buf);
                    } catch (IOException e) {
                        Log.Warn(this, e);
                    }
                    if ( buf.remaining() == 0 && hadResponse.compareAndSet(false,true )) {
                        try {
                            requestChannel.shutdownReads();
                        } catch (IOException e) {
                            Log.Warn(this, e);
                        }
                        exchange.dispatch();
                        parseAndDispatch(exchange, split, rawMethodName, finalM, buf.array(), credentials);
                    }
                }
            );
            requestChannel.resumeReads();
            checkExchangeState(exchange);
        } else {
            exchange.dispatch();
            parseAndDispatch(exchange, split, requestPath, m, new byte[0], credentials );
        }
    }

    private void parseAndDispatch(HttpServerExchange exchange, String[] split, String rawPath, Method m, byte[] postData, Object credentials) {
        try {
            Class<?>[] parameterTypes = m.getParameterTypes();
            Annotation[][] parameterAnnotations = m.getParameterAnnotations();
            Object args[] = new Object[parameterTypes.length];
            int splitIndex = 1;
            try {
                for (int i = 0; i < parameterTypes.length; i++) {
                    Class<?> parameterType = parameterTypes[i];
                    Annotation[] parameterAnnotation = parameterAnnotations[i];
                    if (parameterAnnotation != null && parameterAnnotation.length > 0) {
                        if (parameterAnnotation[0].annotationType() == FromQuery.class) {
                            String value = ((FromQuery) parameterAnnotation[0]).value();
                            Deque<String> strings = exchange.getQueryParameters().get(value);
                            if (strings != null) {
                                args[i] = inferValue(parameterType, strings.getFirst());
                            } else {
                                args[i] = inferValue(parameterType, null);
                            }
                            continue;
                        } else if (parameterAnnotation[0].annotationType() == RequestPath.class) {
                            args[i] = rawPath;
                            continue;
                        } else if (parameterAnnotation[0].annotationType() == AuthCredentials.class) {
                            args[i] = credentials;
                            continue;
                        }
                    }
                    if (splitIndex < split.length) {
                        String stringVal = split[splitIndex];
                        Object val = inferValue(parameterType, stringVal);
                        if (val != NOVAL) {
                            args[i] = val;
                            splitIndex++;
                            continue;
                        }
                    }
                    // specials
                    if (parameterType == HeaderMap.class) {
                        args[i] = exchange.getRequestHeaders();
                    } else if (parameterType == String[].class) {
                        args[i] = split;
                    } else if (postData != null && parameterType == JsonObject.class || parameterType == JsonValue.class) {
                        args[i] = Json.parse(new String(postData, "UTF-8"));
                    } else if (postData != null && parameterType == byte[].class) {
                        args[i] = postData;
                    } else if (parameterType == Map.class) {
                        args[i] = exchange.getQueryParameters();
                    } else {
                        System.out.println("unsupported parameter type " + parameterType.getName());
                    }
                }
            } catch (Throwable th) {
                th.printStackTrace();
                Log.Warn(this,th,postData != null ? new String(postData,0):"");
                exchange.setStatusCode(400);
                exchange.getResponseSender().send(""+th+"\n");
                return;
            }
            // change: allow incomplete parameters
//            if ( splitIndex != split.length ) {
//                exchange.setResponseCode(400);
//                exchange.endExchange();
//                return;
//            }
            Object invoke = m.invoke(facade.getActorRef(), args);
            if ( invoke instanceof IPromise ) {
                checkExchangeState(exchange);
                ((IPromise) invoke).then( (r,e) -> {
                    if ( e != null ) {
                        exchange.setStatusCode(500);
                        exchange.getResponseSender().send(""+e);
                    } else {
                        checkExchangeState(exchange);
                        if ( r instanceof String ) {
                            exchange.setStatusCode(200);
                            exchange.getResponseSender().send(""+r);
                        } else if ( r instanceof Integer ) {
                            exchange.setStatusCode((Integer) r);
                            exchange.endExchange();
                        } else if ( r instanceof Pair ) {
                            exchange.setStatusCode((Integer) ((Pair) r).car());
                            exchange.getResponseSender().send(""+((Pair) r).cdr());
                        } else if ( r instanceof JsonValue ) {
                            exchange.getResponseSender().send(r.toString());
                        } else if ( r instanceof Serializable ) {
                            byte[] bytes = jsonConf.asByteArray(r);
                            exchange.getResponseSender().send(ByteBuffer.wrap(bytes));
                        }
                    }
                });
            } else if (invoke == null) {
                exchange.setStatusCode(200);
                exchange.endExchange();
            }
        } catch (Exception e) {
            Log.Warn(this,e);
            exchange.setStatusCode(500);
            exchange.getResponseSender().send(""+e);
            exchange.endExchange();
            return;
        }
    }

    private void checkExchangeState(HttpServerExchange exchange) {
        if ( false && exchange.isResponseStarted() ) {
            int debug = 1;
            System.out.println("response started "+Thread.currentThread().getName());
        }
    }

    private Object inferValue(Class<?> parameterType, String stringVal) {
        if ( parameterType == int.class ) {
            return stringVal != null ? Integer.parseInt(stringVal) : 0;
        } else if (parameterType == long.class ) {
            return stringVal != null ? Long.parseLong(stringVal) : 0L;
        } else if ( parameterType == double.class ) {
            return stringVal != null ? Double.parseDouble(stringVal) : 0;
        } else if ( parameterType == String.class ) {
            return stringVal;
        } else if ( parameterType == JsonObject.class ) {
            return stringVal != null ? Json.parse(stringVal).asObject() : null;
        } else if ( parameterType == JsonArray.class ) {
            return stringVal != null ? Json.parse(stringVal).asArray() : null;
        } else if ( parameterType == JsonValue.class ) {
            return stringVal != null ? Json.parse(stringVal) : null;
        }
        else if ( parameterType == String[].class ) {
            return stringVal != null ? stringVal.split(",") : null;
        }
        return NOVAL;
    }

}
