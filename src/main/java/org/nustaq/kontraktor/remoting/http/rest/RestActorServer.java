package org.nustaq.kontraktor.remoting.http.rest;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.RemoteCallEntry;
import org.nustaq.kontraktor.remoting.http.*;
import org.nustaq.kontraktor.remoting.http.rest.encoding.JSonMsgCoder;
import org.nustaq.kontraktor.remoting.http.rest.encoding.KsonMsgCoder;
import org.nustaq.kontraktor.remoting.http.rest.encoding.PlainJSonCoder;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.RateMeasure;
import org.nustaq.serialization.util.FSTUtil;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 14.08.2014.
 * implemenst RequestPrccessor for Rest/Http based actor remoting. Requires an actual server implementation
 * (netty or built in NioHttpServer).
 */
public class RestActorServer {

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
    public static <T extends Actor> T publish( Class<T> clz, String path, int port ) {
        T service = Actors.AsActor(clz, 16000);
        return publish(path, port, service);

    }

    /**
     * static utility to publish an actor. Uses built in server.
     */
    static ConcurrentHashMap<Integer,RestActorServer> servers = new ConcurrentHashMap<>(); // FIXME: dirty
    public static <T extends Actor> T publish(String path, int port, T serviceRef) {
        RestActorServer sv = servers.get(port);
        if ( sv == null ) {
            // create Http service abstraction
            sv = new RestActorServer();

            // assign to concrete server impl.
            // a netty based impl is available separately, use internal server here ..
            NioHttpServer server = Actors.AsActor(NioHttpServerImpl.class, 64000);
            sv.startOnServer(port, server);
            servers.put(port,sv);
        }

        // publish service actor
        sv.publish(path,serviceRef);
        return serviceRef;
    }

    public static ArrayList<String> getPublished(String simpleClzName, int port) {
        RestActorServer sv = servers.get(port);
        if ( sv == null )
            return new ArrayList<>();
        return sv.getPublishedActors( simpleClzName );
    }

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

    public ArrayList<String> getPublishedActors( String simpleClzName ) {
        ArrayList<String> result = new ArrayList();
        publishedActors.entrySet().forEach( (entry) -> {
            Actor actor = entry.getValue().getActor().getActor();
            if ( entry.getValue() != null && actor.getClass().getSimpleName().equals(simpleClzName) ) {
                result.add(entry.getKey());
            }
        });
        return result;
    }

    protected void enqueueCall(PublishedActor target, String content, KontraktorHttpRequest req, Callback<RequestResponse> response) {
        try {
            HttpMsgCoder coder = target.getCoder(req.getAccept()) == null ? target.getCoder("text/json") : target.getCoder(req.getAccept());
            final RemoteCallEntry[] calls = coder.decodeFrom(content, req);
            response.receive(RequestResponse.MSG_200, null);
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
                    boolean isContinue = !Actor.isFinal(e);
                    respPerS.count();
                    if ( !isContinue ) {
                        countDown.decrementAndGet();
                    }
                    String fin = countDown.get() <= 0 ? RequestProcessor.FINISHED : null;
                    RemoteCallEntry resCall = new RemoteCallEntry(0, finalCB, "receive", new Object[]{r, "" + e});
                    resCall.setQueue(resCall.CBQ);

                    try {
                        final String encode = coder.encode(resCall);
//                        System.out.println("resp:\n"+encode);
                        response.receive(new RequestResponse(encode), isContinue ? null : fin);
                    } catch (Exception ex) {
                        Log.Warn(this, ex, "");
//                        response.receive(RequestResponse.MSG_500, null);
                        response.receive(new RequestResponse(FSTUtil.toString(ex)), fin);
                    }
                };

                final Class<?>[] parameterTypes = m.getParameterTypes();
                int cbCount = 0; // number of callbacks in args
                for (int ii = 0; ii < parameterTypes.length; ii++) {
                    Class<?> parameterType = parameterTypes[ii];
                    if (Actor.class.isAssignableFrom(parameterType)) {
//                        response.receive(RequestResponse.MSG_500, null);
                        response.receive(new RequestResponse(
                                             "method not http enabled, actor remote references " +
                                                 "cannot be supported for Http based REST (use TCP stack)"),
                                         RequestProcessor.FINISHED
                                        );
                        return;
                    }
                    if (Callback.class.isAssignableFrom(parameterType)) {
                        if (cbCount > 0 || Future.class.isAssignableFrom(m.getReturnType())) {
//                            response.receive(RequestResponse.MSG_500, null);
                            response.receive(new RequestResponse(
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

                Object future = target.getActor().__scheduler.enqueueCall(server.getServingActor(), target.getActor(), call.getMethod(), call.getArgs(), false);
                if (future instanceof Future) {
                    ((Future) future).then(cb);
                } else if (m.getReturnType() == void.class && cbCount == 0) {
                    respPerS.count();
                    if ( countDown.decrementAndGet() == 0 ) {
                        response.receive(null, RequestProcessor.FINISHED);
                    }
                }
            }
        } catch (Exception e) {
            Log.Warn(this,e,"");
            response.receive(RequestResponse.MSG_500, "" + e);
        }
    }

    /**
     * init server and start
     * @param port
     * @param server
     */
    public void startOnServer(int port, NioHttpServer server) {
        this.server = server;
        server.$init(port, new RestProcessor());
        server.$receive();
    }

    /**
     * init server and start
     * @param port
     * @param server
     */
    public void joinServer(int port, NioHttpServer server) {
        this.server = server;
        server.$setHttpProcessor(port, new RestProcessor());
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

        ServeFromCPProcessor cpServer = new ServeFromCPProcessor();

        @Override
        public boolean processRequest(KontraktorHttpRequest req, Callback<RequestResponse> response) {
            if ( req.isGET() ) {
                String actor = req.getPath(0);
                final PublishedActor target = publishedActors.get(actor);
                if ( target == null ) {
                    if ( ! cpServer.processRequest(req,response)) {
                        response.receive(RequestResponse.MSG_404, null);
                        response.receive(null, FINISHED);
                    }
                } else {
                    enqueueCall(target, req, response);
                }
            } else if (req.isPOST() ) {
                String actor = req.getPath(0);
                final PublishedActor target = publishedActors.get(actor);
                if ( target == null ) {
                    response.receive(RequestResponse.MSG_404, null);
                    response.receive(null, FINISHED);
                } else {
                    enqueueCall(target, req.getText().toString(), req, response);
                }
            } else {
                response.receive(RequestResponse.MSG_404, null);
                response.receive(null, FINISHED);
            }
            return true;
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

//    public static void main(String arg[]) {
//        RestActorServer sv = new RestActorServer().map(MDesc.class);
//        sv.publish("rest",Actors.AsActor(RESTActor.class,65000));
//        sv.startOnServer(9999, Actors.AsActor(NioHttpServerImpl.class, 64000));
//    }

}
