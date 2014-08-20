package org.nustaq.kontraktor.remoting.http.rest;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.RemoteCallEntry;
import org.nustaq.kontraktor.remoting.http.*;
import org.nustaq.kontraktor.remoting.http.rest.encoding.JSonMsgCoder;
import org.nustaq.kontraktor.remoting.http.rest.encoding.KsonMsgCoder;
import org.nustaq.kontraktor.remoting.http.rest.encoding.PlainJSonCoder;
import org.nustaq.kontraktor.util.RateMeasure;
import org.nustaq.kson.Kson;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 14.08.2014.
 */
public class RestActorServer {

    NioHttpServer server;
    ConcurrentHashMap<String,PublishedActor> publishedActors = new ConcurrentHashMap<>();

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

    protected void enqueueCall(PublishedActor target, String content, KontraktorHttpRequest req, Callback<RequestResponse> response) {
        try {
            HttpMsgCoder coder = target.getCoder(req.getAccept()) == null ? target.getCoder("text/json") : target.getCoder(req.getAccept());
            final RemoteCallEntry[] calls = coder.decodeFrom(content, req);
            response.receiveResult(RequestResponse.MSG_200, null);
            AtomicInteger countDown = new AtomicInteger(calls.length);
            for (int i = 0; i < calls.length; i++) {
                RemoteCallEntry call = calls[i];

                final Method m = target.getActor().__getCachedMethod(call.getMethod(), target.getActor());
                Callback cb = (r, e) -> {
                    boolean isContinue = Callback.CONTINUE == e;
                    respPerS.count();
                    String fin = countDown.decrementAndGet() <= 0 ? RequestProcessor.FINISHED : null;
                    RemoteCallEntry resCall = new RemoteCallEntry(0, call.getFutureKey(), "receiveResult", new Object[]{r, "" + e});
                    resCall.setQueue(resCall.CBQ);
                    try {
                        response.receiveResult(new RequestResponse(coder.encode(resCall)), isContinue ? null : fin);
                    } catch (Exception ex) {
                        ex.printStackTrace();
//                        response.receiveResult(RequestResponse.MSG_500, null);
                        response.receiveResult(new RequestResponse(ex.toString()), fin);
                    }
                };

                final Class<?>[] parameterTypes = m.getParameterTypes();
                int cbCount = 0; // number of callbacks in args
                for (int ii = 0; ii < parameterTypes.length; ii++) {
                    Class<?> parameterType = parameterTypes[ii];
                    if (Actor.class.isAssignableFrom(parameterType)) {
//                        response.receiveResult(RequestResponse.MSG_500, null);
                        response.receiveResult(new RequestResponse(
                            "method not http enabled, actor remote references " +
                            "cannot be supported for Http based REST (use TCP stack)"),
                            RequestProcessor.FINISHED
                        );
                        return;
                    }
                    if (Callback.class.isAssignableFrom(parameterType)) {
                        if (cbCount > 0 || Future.class.isAssignableFrom(m.getReturnType())) {
//                            response.receiveResult(RequestResponse.MSG_500, null);
                            response.receiveResult(new RequestResponse(
                                "method not http enabled, more than one callback " +
                                "object in args, or callback and also returns future"),
                                RequestProcessor.FINISHED
                            );
                            return;
                        }
                        cbCount++;
                        call.getArgs()[ii] = cb;
                    }
                }

                Object future = target.getActor().__scheduler.enqueueCall(server.getServingActor(), target.getActor(), call.getMethod(), call.getArgs());
                if (future instanceof Future) {
                    ((Future) future).then(cb);
                } else if (m.getReturnType() == void.class && cbCount == 0) {
                    respPerS.count();
                    if ( countDown.decrementAndGet() == 0 )
                        response.receiveResult(null, RequestProcessor.FINISHED);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.receiveResult(RequestResponse.MSG_500, ""+e);
        }
    }

    public void startOnServer(int port, NioHttpServer server) {
        this.server = server;
        server.$init(port, new RestProcessor());
        server.$receive();
    }

    public PublishedActor publish( String name, Actor obj ) {
        PublishedActor pa = new PublishedActor(obj,classNameMappings);
        publishedActors.put(name, pa);
        return pa;
    }

    public void unpublish( String name ) {
        publishedActors.remove(name);
    }

    public class RestProcessor implements RequestProcessor {

        @Override
        public void processRequest(KontraktorHttpRequest req, Callback<RequestResponse> response) {
            if ( req.isGET() ) {
                String actor = req.getPath(0);
                final PublishedActor target = publishedActors.get(actor);
                if ( target == null ) {
                    response.receiveResult(RequestResponse.MSG_404, null);
                    response.receiveResult(null, FINISHED);
                } else {
                    enqueueCall(target, req, response);
                }
            } else if (req.isPOST() ) {
                String actor = req.getPath(0);
                final PublishedActor target = publishedActors.get(actor);
                if ( target == null ) {
                    response.receiveResult(RequestResponse.MSG_404, null);
                    response.receiveResult(null, FINISHED);
                } else {
                    enqueueCall(target, req.getText().toString(), req, response);
                }
            } else {
                response.receiveResult(RequestResponse.MSG_404, null);
                response.receiveResult(null, FINISHED);
            }
        }
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

///////////////////////// test //////////////////////////////////////////////////////////////////////////////////////////

    public static class MDesc {
        String name;
        String returnType;
        String args[];

        public MDesc() {
        }

        public MDesc(String name, String returnType, String[] args) {
            this.name = name;
            this.returnType = returnType;
            this.args = args;
        }

        @Override
        public String toString() {
            return "MDesc{" +
                "name='" + name + '\'' +
                ", returnType='" + returnType + '\'' +
                ", args=" + Arrays.toString(args) +
                '}';
        }
    }

    public static class RESTActor extends Actor<RESTActor> {

        public void simpleCall(String a, String b, int c) {
//            System.out.println("simpleCall "+a+b+c);
        }

        public void withCB(String a, String b, int c, Callback cb) {
            cb.receiveResult("withCB "+a+b+c,null);
        }

        public Future simpleFut(String a, String b, int c) {
            Thread.currentThread().setName("RESTActor");
            final Method[] methods = getClass().getMethods();
            ArrayList res = new ArrayList();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                final int mods = method.getModifiers();
                if ( !Modifier.isStatic(mods) && ! Modifier.isAbstract(mods) && Modifier.isPublic(mods)
                        && method.getDeclaringClass() != Object.class && method.getDeclaringClass() != Actor.class ) {
                    final Class<?>[] parms = method.getParameterTypes();
                    String parmTypes[] = new String[parms.length];
                    for (int j = 0; j < parmTypes.length; j++) {
                        parmTypes[j] = parms[j].getSimpleName();
                    }
                    res.add( new MDesc(method.getName(),method.getReturnType().getSimpleName(),parmTypes) );
                }
            }
            return new Promise<>(res);
        }
    }

    public static void main(String arg[]) {
        RestActorServer sv = new RestActorServer().map(MDesc.class);
        sv.publish("rest",Actors.AsActor(RESTActor.class,65000));
        sv.startOnServer(9999, Actors.AsActor(NioHttpServerImpl.class, 64000));
    }

}
