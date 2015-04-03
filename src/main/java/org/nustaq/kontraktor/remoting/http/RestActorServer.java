package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.RemoteCallEntry;
import org.nustaq.kontraktor.remoting.http.encoding.JSonMsgCoder;
import org.nustaq.kontraktor.remoting.http.encoding.KsonMsgCoder;
import org.nustaq.kontraktor.remoting.http.encoding.PlainJSonCoder;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.RateMeasure;
import org.nustaq.serialization.util.FSTUtil;

import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

/**
 * Created by ruedi on 14.08.2014.
 * implemenst Rest/Http based actor remoting. Requires an actual server implementation adapter
 * (netty or undertow currently).
 */
public class RestActorServer {

    public static String FINISHED = "FIN"; // must be sent once all responses are sent

    /**
     * shorthand to create and run a http service publishing given actor class.
     * For more control (queue sizes, several actors on a single port, use a distinct server impl) do a stepwise
     * init (just checkout source of method)
     *
     * @param clz
     * @param path
     * @param port
     * @param <T>
     * @return
     */
//    public static <T extends Actor> T Publish(Class<T> clz, String path, int port) {
//        T service = Actors.AsActor(clz, 16000);
//        return Publish(path, port, service);
//
//    }

    /**
     * static utility to Publish an actor. Uses built in server.
     */
    static ConcurrentHashMap<Integer,RestActorServer> servers = new ConcurrentHashMap<>(); // FIXME: dirty
//    public static <T extends Actor> T Publish(String path, int port, T serviceRef) {
//        RestActorServer sv = getRestActorServer(port);
//        if ( sv == null ) {
//            // create Http service abstraction
//            sv = new RestActorServer();
//
//            // assign to concrete server impl.
//            // a netty based impl is available separately, use internal server here ..
//            NioHttpServer server = Actors.AsActor(NioHttpServerImpl.class, 64000);
//            sv.startOnServer(port, server);
//            servers.put(port,sv);
//        }
//
//        // Publish service actor
//        sv.publish(path,serviceRef);
//        return serviceRef;
//    }

    public static RestActorServer getRestActorServer(int port) {
        return servers.get(port);
    }

    public static ArrayList<String> getPublished(String simpleClzName, int port) {
        RestActorServer sv = getRestActorServer(port);
        if ( sv == null )
            return new ArrayList<>();
        return sv.getPublishedActors( simpleClzName );
    }

    NioHttpServer server;
    ConcurrentHashMap<String,PublishedActor> publishedActors = new ConcurrentHashMap<>();
    BiFunction<Actor,String,Boolean> remoteCallInterceptor;
    RestProcessor restProcessor;

    public BiFunction<Actor, String, Boolean> getRemoteCallInterceptor() {
        return remoteCallInterceptor;
    }

    public void setRemoteCallInterceptor(BiFunction<Actor, String, Boolean> remoteCallInterceptor) {
        this.remoteCallInterceptor = remoteCallInterceptor;
    }

    public RestActorServer() {
    }

    RateMeasure respPerS = new RateMeasure("responaes/s", 1000);
    protected void enqueueCall(PublishedActor target, KontraktorHttpRequest req, Callback<RequestResponse> response) {
        final Method m = target.getActor().__getCachedMethod(req.getPath(1), target.getActor());
        if ( m == null ) {
            throw new RuntimeException("no such method '"+req.getPath(1)+"' on "+target.getActor().getClass().getSimpleName());
        }
        int args = m.getParameterCount();
        final Class<?>[] parameterTypes = m.getParameterTypes();
        String json = "[ { method: "+req.getPath(1)+" args: [ ";
        for (int i=0; i < args; i++) {
            String path = req.getPath(i + 2);
            if ( (!parameterTypes[i].isPrimitive() || !Number.class.isAssignableFrom(parameterTypes[i]))
                && ! path.startsWith("'") && path.startsWith("{"))
                path = "'"+path+"'";
            if ( path.equals("") ) {
                if ( parameterTypes[i].isPrimitive() ) {
                    Class<?> par = parameterTypes[i];
                    if (par == byte.class || par == short.class || par == char.class || par == int.class || par == long.class ) {
                        path = "0";
                    } else if ( par == float.class || par == double.class ) {
                        path = "0.0";
                    } else if ( par == boolean.class )
                        path = "false";
                    else
                        path = "null";
                } else {
                    path = "null";
                }
            }
            json += path + " ";
        }
        json+="] } ]";
        enqueueCall(target, json, req, response);
    }

    public ArrayList<String> getPublishedActors( String simpleClzName ) {
        ArrayList<String> result = new ArrayList();
        publishedActors.entrySet().forEach((entry) -> {
            Actor actor = entry.getValue().getActor().getActor();
            if (entry.getValue() != null && actor.getClass().getSimpleName().equals(simpleClzName)) {
                result.add(entry.getKey());
            }
        });
        return result;
    }

    protected void enqueueCall(PublishedActor target, String content, KontraktorHttpRequest req, Callback<RequestResponse> response) {
        try {
            HttpMsgCoder coder = target.getCoder(req.getAccept()) == null ? target.getCoder("text/json") : target.getCoder(req.getAccept());
            content = URLDecoder.decode(content, "UTF-8");
            final RemoteCallEntry[] calls = coder.decodeFrom(content, req);
            // special name for http redirect. Quirky, however let's be practical ;)
            // method is expected to have no callbacks in argument and to return
            // a Future<HtmlString>. The HtmlString has to contain the new url
            if ( calls.length == 1 && calls[0].getMethod().startsWith("$httpRedirect")) {
                RemoteCallEntry call = calls[0];
                Callback cb = (r, e) -> {
                    if ( r != null ) {
                        response.complete(RequestResponse.MSG_302(((HtmlString) r).getRedirectUrl()), null);
                        response.complete(new RequestResponse(""), FINISHED);
                    } else {
                        Log.Warn(this, "error in httpRedirect "+e);
                        response.complete(RequestResponse.MSG_500, FINISHED);
                    }
                };
                try {
                    target.getActor().__getCachedMethod(call.getMethod(), target.getActor());
                    IPromise future = (IPromise) target.getActor().__scheduler.enqueueCall(server.getServingActor(), target.getActor(), call.getMethod(), call.getArgs(), false);
                    future.then(cb);
                } catch (Exception e) {
                    Log.Warn(this,e);
                }
                return;
            }
            response.complete(RequestResponse.MSG_200, null);
            AtomicInteger countDown = new AtomicInteger(calls.length);
            for (int i = 0; i < calls.length; i++) {
                RemoteCallEntry call = calls[i];

                // find cbid for callbacks
                int cbid = call.getFutureKey();
                final Object[] args = call.getArgs();
                for (int ii = 0; ii < args.length; ii++) {
                    Object o = args[ii];
                    if ( o instanceof Actor ) {
                        throw new RuntimeException("remote actor references are not supported via http, use TCP stack");
                    } else
                    if ( o instanceof HttpRemotedCB ) {
                        cbid = ((HttpRemotedCB) o).getCbid();
                    }
                }

                int finalCB = cbid;
                final Method m = target.getActor().__getCachedMethod(call.getMethod(), target.getActor());
                Callback cb = (r, e) -> {
                    //FIXME: termination logic is contrary to kontraktor: only if error occurs or error = RequestProcessor.FINISHED
                    //FIXME: response will be closed.
                    boolean isContinue = !Actor.isFinal(e) && !(r instanceof HtmlString);
                    respPerS.count();
                    if ( !isContinue ) {
                        countDown.decrementAndGet();
                    }
                    String fin = countDown.get() <= 0 ? FINISHED : null;
                    if ( r instanceof HtmlString ) {
                        response.complete(new RequestResponse((HtmlString) r), fin); // note: pipelining does not make sense with HtmlString results
                        return;
                    }
                    RemoteCallEntry resCall = new RemoteCallEntry(0, finalCB, "complete", new Object[]{r, e == null ? null : ("" + e) });
                    resCall.setQueue(resCall.CBQ);

                    try {
                        final String encode = coder.encode(resCall);
//                        System.out.println("resp:\n"+encode);
                        response.complete(new RequestResponse(encode), isContinue ? null : fin);
                    } catch (Exception ex) {
                        Log.Warn(this, ex, "");
//                        response.complete(RequestResponse.MSG_500, null);
                        response.complete(new RequestResponse(FSTUtil.toString(ex)), fin);
                    }
                };

                final Class<?>[] parameterTypes = m.getParameterTypes();
                int cbCount = 0; // number of callbacks in args
                for (int ii = 0; ii < parameterTypes.length; ii++) {
                    Class<?> parameterType = parameterTypes[ii];
                    if (Actor.class.isAssignableFrom(parameterType)) {
//                        response.complete(RequestResponse.MSG_500, null);
                        response.complete(new RequestResponse(
                                                                 "method not http enabled, actor remote references " +
                                                                     "cannot be supported for Http based REST (use TCP stack)"),
                                             FINISHED
                        );
                        return;
                    }
                    if (Callback.class.isAssignableFrom(parameterType)) {
                        if (cbCount > 0 || IPromise.class.isAssignableFrom(m.getReturnType())) {
//                            response.complete(RequestResponse.MSG_500, null);
                            response.complete(new RequestResponse(
                                                                     "method not http enabled, more than one callback " +
                                                                         "object in args, or callback and also returns future"),
                                                 FINISHED
                            );
                            return;
                        }
                        cbCount++;
                        call.getArgs()[ii] = cb;
                    }
                }

                Object future = target.getActor().__scheduler.enqueueCall(server.getServingActor(), target.getActor(), call.getMethod(), call.getArgs(), false);
                if (future instanceof IPromise) {
                    ((IPromise) future).then(cb);
                } else if (m.getReturnType() == void.class && cbCount == 0) {
                    respPerS.count();
                    if ( countDown.decrementAndGet() == 0 ) {
                        response.complete(null, FINISHED);
                    }
                }
            }
        } catch (Exception e) {
            Log.Warn(this,e,"");
            response.complete(RequestResponse.MSG_500, "" + e);
        }
    }

    /**
     * init server and start
     * @param port
     * @param server
     */
    public void startOnServer(int port, NioHttpServer server) {
        this.server = server;
        server.$init(port, new RestProcessor(this));
        server.$receive();
    }

    /**
     * set self as a http processor for a running server instance
     * @param server
     */
    public void joinServer(NioHttpServer server) {
        this.server = server;
        server.$setHttpProcessor(this.restProcessor = new RestProcessor(this));
    }

    public PublishedActor publish( String name, Actor obj ) {
        PublishedActor pa = new PublishedActor(obj,classNameMappings);
        publishedActors.put(name, pa);
        return pa;
    }

    public void unpublish( String name ) {
        publishedActors.remove(name);
    }

    ConcurrentHashMap<String,Class> classNameMappings = new ConcurrentHashMap<>();

    public RestActorServer map( String s, Class clz ) {
        classNameMappings.put(s,clz);
        return this;
    }

    public RestActorServer map( Class ... clz ) {
        for (int i = 0; i < clz.length; i++) {
            Class aClass = clz[i];
            map(clz[i].getSimpleName(),clz[i]);
        }
        return this;
    }

    public static class PublishedActor {

        Actor actor;
        HashMap<String,HttpMsgCoder> coders = new HashMap<>();

        public PublishedActor(Actor actor, Map<String,Class> mappings) {
            this.actor = actor;
            KsonMsgCoder kson = new KsonMsgCoder(actor.getActor().getClass());
            KsonMsgCoder json = new JSonMsgCoder(actor.getActor().getClass());
            KsonMsgCoder plainJson = new PlainJSonCoder(actor.getActor().getClass());

            mappings.forEach( (name,clz) -> {
                kson.map(name,clz);
                json.map(name,clz);
                plainJson.map(name,clz);
            });

            coders.put("text/kson", kson);
            coders.put("text/json", plainJson);
            coders.put("text/json-tagged", json);
        }

        public PublishedActor putCoder(String name, HttpMsgCoder coder) {
            coders.put(name, coder);
            return this;
        }

        public HttpMsgCoder getCoder(String name) {
            return coders.get(name);
        }

        public Actor getActor() {
            return actor;
        }
    }

//    public static void main(String arg[]) {
//        RestActorServer sv = new RestActorServer().then(MDesc.class);
//        sv.Publish("rest",Actors.AsActor(RESTActor.class,65000));
//        sv.startOnServer(9999, Actors.AsActor(NioHttpServerImpl.class, 64000));
//    }

}
