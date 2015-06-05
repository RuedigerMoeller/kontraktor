package org.nustaq.kontraktor.barebone;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.coders.Unknown;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by ruedi on 05/06/15.
 */
public class RemoteActorConnection {

    public static boolean DumpProtocol = true;

    protected static CloseableHttpAsyncClient asyncHttpClient;
    public static CloseableHttpAsyncClient getClient() {
        synchronized (RemoteActorConnection.class) {
            if (asyncHttpClient == null ) {
                asyncHttpClient = HttpAsyncClients.custom()
                    .setDefaultIOReactorConfig(
                        IOReactorConfig.custom()
                            .setIoThreadCount(1)
                            .setSoKeepAlive(true)
                            .build()
                    ).build();
                asyncHttpClient.start();
            }
            return asyncHttpClient;
        }
    }

    final static Header NO_CACHE = new Header() {
        @Override
        public String getName() {
            return "Cache-Control";
        }
        @Override
        public String getValue() {
            return "no-cache";
        }
        @Override
        public HeaderElement[] getElements() throws ParseException {
            return new HeaderElement[0];
        }
    };

    FSTConfiguration conf;
    ScheduledExecutorService myThread = Executors.newSingleThreadScheduledExecutor();
    ScheduledExecutorService longPoller = Executors.newSingleThreadScheduledExecutor();
    String sessionId;
    String sessionUrl;
    int lastSeenSeq;


    // placeholder for serialized kontraktor classes
    public static class _Timeout implements Serializable {}

    public RemoteActorConnection(boolean sharedRefs) {
        initConf(sharedRefs);
        try {
            if ( Class.forName("org.nustaq.kontraktor.Actor") != null ) {
                throw new RuntimeException("this client library clashes with full kontraktor release. Use standard kontraktor client if its on the classpath anyway.");
            }
        } catch (ClassNotFoundException e) {
            // expected
        }
    }

    void initConf(boolean sharedRefs) {
        // support Json encoding only in order to deal with unknown classes
        conf = FSTConfiguration.createJsonConfiguration(DumpProtocol, !DumpProtocol && sharedRefs);
        conf.registerCrossPlatformClassMapping(new String[][]{
            {"call", BBRemoteCallEntry.class.getName()},
            {"cbw", BBCallback.class.getName()}
        });
        conf.registerSerializer(BBCallback.class,new BBCallbackRefSerializer(this),true);
    }

    public BBPromise<RemoteActor> connect(final String url, boolean longPoll) {
        final BBPromise res = new BBPromise();

        byte[] message = conf.asByteArray(null);
        if (DumpProtocol) {
            try {
                System.out.println("auth-req:"+new String(message,"UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        HttpPost req = createRequest(url, message);
        getClient().execute(req, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse result) {
                if (result.getStatusLine().getStatusCode() != 200) {
                    return;
                }
                String cl = result.getFirstHeader("Content-Length").getValue();
                int len = Integer.parseInt(cl);
                if (len > 0) {
                    final byte resp[] = new byte[len];
                    try {
                        result.getEntity().getContent().read(resp);
                        if (DumpProtocol) {
                            try {
                                System.out.println("auth-resp:" + new String(resp, "UTF-8"));
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                        myThread.execute(new Runnable() {
                            @Override
                            public void run() {
                                sessionId = (String) conf.asObject(resp);
                                sessionUrl = url+"/"+sessionId;
                                System.out.println("session id:" + sessionId);
                                res.complete(new RemoteActor("App",1,RemoteActorConnection.this),null);
                            }
                        });
                    } catch (Exception e) {
                        res.complete(null,e);
                        if ( DumpProtocol )
                            e.printStackTrace();
                    }
                } else {
                    res.complete(null,"connection rejected, no connection id");
                }
            }

            @Override
            public void failed(Exception ex) {
                res.complete(null,ex);
            }

            @Override
            public void cancelled() {
                res.complete(null,"connection failed. Canceled request");
            }
        });

        if ( longPoll ) {
            final AtomicReference<Runnable> lp = new AtomicReference<>();
            lp.set(new Runnable() {
                @Override
                public void run() {
                    longPoller.schedule(lp.get(), 1000, TimeUnit.MILLISECONDS);
                }
            });
            longPoller.schedule(lp.get(), 1000, TimeUnit.MILLISECONDS);
        }
//        myThread.submit(new Runnable() {
//            @Override
//            public void run() {
//                res.complete(null,null);
//            }
//        });
        return res;
    }

    ConcurrentHashMap<Integer,BBCallback> callbackMap = new ConcurrentHashMap<>();
    AtomicInteger idCount = new AtomicInteger(0);
    protected void addRequest(BBRemoteCallEntry remoteCallEntry, BBPromise res) {
        if ( res != null ) {
            int key = registerCallback(res);
            remoteCallEntry.futureKey = key;
        }
        Object req[] = new Object[] { remoteCallEntry, 0 };
        HttpPost request = createRequest(sessionUrl, conf.asByteArray(req));
        getClient().execute(request, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse result) {
                if (result.getStatusLine().getStatusCode() == 404) {
//                    closeClient();
//                    p.reject("Closed");
                    return;
                }
                String cl = result.getFirstHeader("Content-Length").getValue();
                int len = Integer.parseInt(cl);
                if (len > 0) {
                    byte b[] = new byte[len];
                    try {
                        result.getEntity().getContent().read(b);

                        if ( DumpProtocol ) {
                            try {
                                System.out.println("resp:"+new String(b,"UTF-8"));
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }

                        Object o[] = (Object[]) conf.asObject(b);
                        int seq = ((Number) o[o.length - 1]).intValue();
                        if ( seq == lastSeenSeq+1 || lastSeenSeq == 0 ) {
                            lastSeenSeq = seq;
                            for (int i = 0; i < o.length-1; i++) {
                                BBRemoteCallEntry call = (BBRemoteCallEntry) o[i];
                                if ( call.getQueue() == 1 ) // callback
                                {
                                    // catch and transform remote actor reference
                                    if ( call.getArgs()[0] instanceof Unknown ) {
                                        Unknown uk = (Unknown) call.getArgs()[0];
                                        List items = uk.getItems();
                                        if ( items.size() == 2 ) {
                                            if ( items.get(0) instanceof Number &&
                                                 items.get(1) instanceof String &&
                                                 ((String) items.get(1)).endsWith("_ActorProxy")
                                               ) {
                                                // actor proxy detected
                                                String actorName = (String) items.get(1);
                                                actorName = actorName.substring(0,actorName.length()-"_ActorProxy".length());
                                                call.getArgs()[0] = new RemoteActor(
                                                    actorName,
                                                    ((Number) items.get(0)).intValue(),
                                                    RemoteActorConnection.this
                                                );
                                            }
                                        }
                                    }
                                    // process future callback or callback
                                    BBCallback bbCallback = callbackMap.get(call.getReceiverKey());
                                    if ( bbCallback == null ) {
                                        log("unknown callback receiver "+call);
                                    } else {
                                        callbackMap.remove(call.getReceiverKey());
                                        bbCallback.receive(call.getArgs()[0],call.getArgs()[1]);
                                    }
                                }
                            }
                        } else {
                            log("ignored result with sequence:" + seq + " lastSeen:" + lastSeenSeq);
                        }
                    } catch (Throwable e) {
                        log(e);
                    }
                }
            }

            @Override
            public void failed(Exception ex) {

            }
            @Override
            public void cancelled() {

            }
        });
    }

    public int registerCallback(BBCallback res) {
        int key = idCount.incrementAndGet();
        callbackMap.put(key, res );
        return key;
    }

    private void log(Throwable e) {
        e.printStackTrace();
    }

    protected void log(String s) {
        System.out.println(s);
    }

    protected HttpPost createRequest(String url, byte[] message) {
        HttpPost req = new HttpPost(url);
        req.addHeader(NO_CACHE);
        req.setEntity(new ByteArrayEntity(message));
        return req;
    }

    public static void main(String[] args) throws InterruptedException {
        RemoteActorConnection act = new RemoteActorConnection(false);
        act.connect("http://localhost:8080/api",false).then(new BBCallback<RemoteActor>() {
            @Override
            public void receive(RemoteActor result, Object error) {
                System.out.println("result:" + result+" err:"+error );
                result.ask("login", "user", "password").then(new BBCallback<RemoteActor>() {
                    @Override
                    public void receive(RemoteActor session, Object error) {
                        System.out.println("session Actor received: "+session);
                        session.ask("getToDo").then(new BBCallback<ArrayList>() {
                            @Override
                            public void receive(ArrayList result, Object error) {
                                for (int i = 0; i < result.size(); i++) {
                                    Object o = result.get(i);
                                    System.out.println(o);
                                }
                            }
                        });
                    }
                });
            }
        });
        Thread.sleep(100000);
    }

}
