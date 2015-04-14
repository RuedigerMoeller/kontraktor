package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.annotations.Register;
import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.impl.RemoteScheduler;
import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.kontraktor.remoting.RemoteCallEntry;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.util.Log;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ruedi on 14.08.2014.
 */
public class RestActorClient<T extends Actor> extends RemoteRefRegistry {

    T facadeProxy;
    int port = 9999;
    String host;
    String actorPath;
    Class<T> actorClazz;
    HttpObjectSocket channel;
    ConcurrentHashMap<String,Class> mappings = new ConcurrentHashMap<>();

    public RestActorClient( String host, int port, String actorPath, Class clz) {
        this.port = port;
        this.host = host;
        this.actorPath = actorPath;
        this.actorClazz = clz;
        facadeProxy = Actors.AsActor(actorClazz, new RemoteScheduler());
        facadeProxy.__remoteId = 0;
        registerRemoteRefDirect(facadeProxy);
        Register toReg = (Register) clz.getAnnotation(Register.class);
        if (toReg!=null) {
            map(toReg.value());
        }
    }

    public T getFacadeProxy() {
        return facadeProxy;
    }

    BackOffStrategy backOffStrategy = new BackOffStrategy();
    protected void sendLoop(ObjectSocket channel) throws Exception {
        try {
            int count = 0;
            while (!isTerminated()) {
                if (singleSendLoop(channel)) {
                    count = 0;
                }
                backOffStrategy.yield(count++);
            }
        } finally {
            stopRemoteRefs();
        }
    }


    public RestActorClient<T> connect() {
        channel = new HttpObjectSocket(actorClazz, port, host, actorPath);
        mappings.forEach( (k,v) -> channel.getKson().map(k,v) );
        new Thread(
            () -> {
                try {
                    sendLoop(channel);
                } catch (Exception e) {
                    Log.Warn(this, e, "");
                }
            },
            "httpclient:sender"
        ).start();
        new Thread(
            () -> {
                receiveLoop(channel);
            },
            "httpclient:receiver"
        ).start();
        return this;
    }

    @Override
    protected void writeObject(ObjectSocket chan, RemoteCallEntry rce) throws Exception {
        final Object[] args = rce.getArgs();
        for (int i = 0; i < args.length; i++) {
            Object o = args[i];
            if ( o instanceof Callback ) {
                int cbid = registerPublishedCallback((Callback) o);
                args[i] = new HttpRemotedCB(cbid);
            }
            if ( o instanceof Actor ) {
                throw new RuntimeException("remote actor references are not supported via http");
            }
        }
        super.writeObject(chan, rce);
    }

    public RestActorClient<T> map( String s, Class clz ) {
        mappings.put(s, clz);
        if (channel !=null) {
            channel.getKson().map(s,clz);
        }
        return this;
    }

    public RestActorClient<T> map( Class ... clz ) {
        for (int i = 0; i < clz.length; i++) {
            Class aClass = clz[i];
            map(clz[i].getSimpleName(),clz[i]);
        }
        return this;
    }


//    public static void main(String a[]) {
//        RestActorClient<RestActorServer.RESTActor> cl = new RestActorClient("localhost", 9999, "/rest", RestActorServer.RESTActor.class);
//        cl.connect();
//        final RestActorServer.RESTActor proxy = cl.getFacadeProxy();
//        int count =0;
//        while( true )
//        {
////            proxy.simpleCall("A", "B", 133);
//            proxy.simpleCall("C", "D", 133);
////            proxy.simpleFut("a","b",31).then((r,e)-> {
////                System.out.println("result:"+r+", "+e);
////            });
//            count++;
//            if ( (count%5) == 1 )
//                LockSupport.parkNanos(1000 * 1000 * 1L);
//        }
//    }
}
