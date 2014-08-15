package org.nustaq.kontraktor.remoting.http.rest;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.RemoteCallEntry;
import org.nustaq.kontraktor.remoting.http.*;
import org.nustaq.kontraktor.util.RateMeasure;
import org.nustaq.kson.Kson;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 14.08.2014.
 */
public class RestActorServer {

    NioHttpServer server;
    ConcurrentHashMap<String,Actor> publishedActors = new ConcurrentHashMap<>();
    private boolean useKson = false;

    RateMeasure respPerS = new RateMeasure("responaes/s", 1000);
    class RestProcessor implements RequestProcessor {
        @Override
        public void processRequest(KontraktorHttpRequest req, Callback<RequestResponse> response) {
            if ( req.isGET() ) {
                String actor = req.getPath(0);
                final Actor target = publishedActors.get(actor);
                if ( target == null ) {
                    response.receiveResult(RequestResponse.MSG_404, null);
                    response.receiveResult(null, FINISHED);
                } else {
                    enqueueCall(target, req, response);
                }
            } else if (req.isPOST() ) {
                String actor = req.getPath(0);
                final Actor target = publishedActors.get(actor);
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

    public boolean isUseKson() {
        return useKson;
    }

    public RestActorServer setUseKson(boolean useKson) {
        this.useKson = useKson;
        return this;
    }

    protected void enqueueCall(Actor target, KontraktorHttpRequest req, Callback<RequestResponse> response) {
        final Method m = target.__getCachedMethod(req.getPath(1), target);
        int args = m.getParameterCount();
        final Class<?>[] parameterTypes = m.getParameterTypes();
        String json = "[ { method: "+req.getPath(1)+" args: [ ";
        for (int i=0; i < args; i++) {
            String path = req.getPath(i + 2);
            if ( parameterTypes[i] == String.class && ! path.startsWith("'"))
                path = "'"+path+"'";
            if ( path.equals("") )
                path = "null";
            json += path + " ";
        }
        json+="] } ]";
        enqueueCall(target, json, req, response);
    }

    protected void enqueueCall(Actor target, String jsonString, KontraktorHttpRequest req, Callback<RequestResponse> response) {
        try {
            Kson kson = new Kson().map("call", RemoteCallEntry.class).map("calls", RemoteCallEntry[].class);

            ArgTypesResolver res = new ArgTypesResolver( target.getActor().getClass() );
//            System.out.println(jsonString);
            final RemoteCallEntry[] calls = (RemoteCallEntry[])kson.readObject(jsonString, "calls", res);
            response.receiveResult(RequestResponse.MSG_200, null);
            AtomicInteger countDown = new AtomicInteger(calls.length);
            for (int i = 0; i < calls.length; i++) {
                RemoteCallEntry call = calls[i];

                final Method m = target.__getCachedMethod(call.getMethod(), target);

                Callback cb = (r, e) -> {
                    respPerS.count();
                    String fin = countDown.decrementAndGet() == 0 ? RequestProcessor.FINISHED : null;
                    RemoteCallEntry resCall = new RemoteCallEntry(0, call.getFutureKey(), "receiveResult", new Object[]{r, "" + e});
                    resCall.setQueue(resCall.CBQ);
                    try {
                        if (useKson) {
                            response.receiveResult(new RequestResponse(kson.writeObject(resCall)), fin);
                        } else {
                            response.receiveResult(new RequestResponse(kson.writeJSonObject(resCall, false)), fin);
                        }
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

                Object future = target.__scheduler.enqueueCall(server, target, call.getMethod(), call.getArgs());
                if (future instanceof Future) {
                    ((Future) future).then(cb);
                } else if (m.getReturnType() == void.class && cbCount == 0) {
                    respPerS.count();
                    response.receiveResult(null, countDown.decrementAndGet() == 0 ? RequestProcessor.FINISHED : null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void startServer() {
        server = Actors.AsActor(NioHttpServer.class,64000);
        server.$init(9999, new RestProcessor());
        server.$receive();
    }

    public void publish( String name, Actor obj ) {
        publishedActors.put(name,obj);
    }

    public void unpublish( String name ) {
        publishedActors.remove(name);
    }

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
            System.out.println("simpleCall "+a+b+c);
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
        RestActorServer sv = new RestActorServer().setUseKson(true);
        sv.publish("rest",Actors.AsActor(RESTActor.class,65000));
        sv.startServer();
    }

}
