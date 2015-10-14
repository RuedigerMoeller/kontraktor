/*
 * Copyright 2014 Ruediger Moeller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by ruedi on 05/06/15.
 *
 * A Json encoded Http Long Poll Client
 *
 * Note all callbacks are delivered in the same single thread.
 *
 * NEVER BLOCK THIS THREAD.
 *
 * Use an ExecutorService to perform blocking code or code with longer duration
 * from within a callback.
 *
 * Objects of unknown (not on classpath) classes returned / required by the Remote Actor are translated
 * into "Unknown" instances. Note that its favourable to create kind of a shared clientprotocol.jar to get
 * properly deserlialized and typed results.
 *
 */
public class RemoteActorConnection {

    public static final int LONG_POLL_MAX_TIME = 15_000;
    public static int MAX_CONN_PER_ROUTE = 1000;
    public static int MAX_CONN_TOTAL = 1000;
    public static boolean DumpProtocol = false;

    protected static CloseableHttpAsyncClient asyncHttpClient;
    protected volatile boolean isConnected;
    private static final boolean SENDDEBUG = false;

    public CloseableHttpAsyncClient getClient() {
        synchronized (this) {
            if (asyncHttpClient == null ) {
                asyncHttpClient = HttpAsyncClients.custom()
                    .setMaxConnPerRoute(MAX_CONN_PER_ROUTE)
                    .setMaxConnTotal(MAX_CONN_TOTAL)
                    .setDefaultIOReactorConfig(
                        IOReactorConfig.custom()
                            .setIoThreadCount(1)
                            .setSoKeepAlive(true)
                            .setSoReuseAddress(true)
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

    protected FSTConfiguration conf;
    protected static ExecutorService myExec = Executors.newSingleThreadExecutor();
    protected String sessionId;
    protected String sessionUrl;
    protected int lastSeenSeq;
    protected ConnectionListener connectionListener;
    protected volatile long timeout = LONG_POLL_MAX_TIME*2; // signal close if no longpoll has been received for this time

    protected long lastPing;
    /**
     * callback id => promise or callback
     */
    protected ConcurrentHashMap<Long,Callback> callbackMap = new ConcurrentHashMap<>();
    /**
     * used to generate unique ids for callbacks/promises/actors
     */
    protected AtomicLong idCount = new AtomicLong(0);
    protected boolean requestUnderway = false; // avoid opening a second http connection
    /**
     * buffered requests to be sent, will be batched
     */
    protected ArrayList<RemoteCallEntry> requests = new ArrayList<>();
    /**
     * number of unreplied 'ask' calls. Backpressure can be done by
     * observing this. Its important your server app always returns a result or error (no silent timeout),
     * else callbacks will stack up and this count will increase. Backpressure by limiting open futures
     * then at some point will stall the client.
     * TODO: add time based timeout for unreplied futures.
     */
    protected AtomicInteger openFutureRequests = new AtomicInteger(0);

    /**
     * create a default configures (json serialization, no shared refs, no pretty print)
     * @param connectionListener
     */
    public RemoteActorConnection(ConnectionListener connectionListener) {
        this(connectionListener,false);
    }

    /**
     * note that shared refs support requires the server to be also configured for json shared refs.
     * as currenlty the js client does not support this, its likely server run with shared refs false.
     *
     * @param sharedRefs
     */
    public RemoteActorConnection(ConnectionListener connectionListener,boolean sharedRefs) {
        initConf(sharedRefs);
        this.connectionListener = connectionListener;
        try {
            if ( Class.forName("org.nustaq.kontraktor.Actor") != null ) {
                throw new RuntimeException("this client library clashes with full kontraktor release. Use standard kontraktor client if its on the classpath anyway.");
            }
        } catch (ClassNotFoundException e) {
            // expected
        }
        myExec.execute(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("kontraktor-bare client");
            }
        });
    }

    public ConnectionListener getConnectionListener() {
        return connectionListener;
    }

    protected void initConf(boolean sharedRefs) {
        // support Json encoding only in order to deal with unknown classes
        conf = FSTConfiguration.createJsonConfiguration(DumpProtocol, !DumpProtocol && sharedRefs);
        conf.registerCrossPlatformClassMapping(new String[][]{
            {"call", RemoteCallEntry.class.getName()},
            {"cbw", Callback.class.getName()}
        });
        conf.registerSerializer(Callback.class, new CallbackRefSerializer(this), true);
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public Promise<RemoteActor> connect(final String url, final boolean longPoll) {
        final Promise res = new Promise();

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
                lastPing = System.currentTimeMillis();
                if (result.getStatusLine().getStatusCode() != 200) {
                    res.receive(null,"connection failed with status:"+result.getStatusLine().getStatusCode());
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
                        myExec.execute(new Runnable() {
                            @Override
                            public void run() {
                                sessionId = (String) conf.asObject(resp);
                                isConnected = true;
                                if ( longPoll ) {
                                    startLongPoll();
                                }
                                sessionUrl = url + "/" + sessionId;
                                System.out.println("session id:" + sessionId);
                                res.complete(new RemoteActor("App", 1, RemoteActorConnection.this), null);
                            }
                        });
                    } catch (Exception e) {
                        res.complete(null, e);
                        if (DumpProtocol)
                            e.printStackTrace();
                    }
                } else {
                    res.complete(null, "connection rejected, no connection id");
                }
            }

            @Override
            public void failed(Exception ex) {
                res.complete(null, ex);
            }

            @Override
            public void cancelled() {
                res.complete(null, "connection failed. Canceled request");
            }
        });

        return res;
    }

    protected void startLongPoll() {
        final AtomicReference<Runnable> lp = new AtomicReference<>();
        lp.set(new Runnable() {
            @Override
            public void run() {
                if ( ! isConnected )
                    return;

                final AtomicInteger timedout = new AtomicInteger(0); // 1 = reply, 2 = timeout
                delayed(new Runnable() {
                    @Override
                    public void run() {
                        checkTimeout();
                        if ( timedout.compareAndSet(0,2) ) {
                            // long poll timeout, retry
                            myExec.execute(lp.get());
                        }
                    }
                }, LONG_POLL_MAX_TIME + 1000 ); // give 1 second trip latency
                HttpPost request = createRequest(sessionUrl, conf.asByteArray(new Object[] { lastSeenSeq } ));
                getClient().execute(request, new FutureCallback<HttpResponse>() {
                    @Override
                    public void completed(HttpResponse result) {
                        if (!timedout.compareAndSet(0, 1)) {
                            return;
                        }
                        if (result.getStatusLine().getStatusCode() != 200) {
                            log("unexpected return status " + result.getStatusLine().getReasonPhrase());
                            delayed(lp.get(), 2000);
                            return;
                        }
                        lastPing = System.currentTimeMillis();
                        String cl = result.getFirstHeader("Content-Length").getValue();
                        int len = Integer.parseInt(cl);
                        if (len > 0) {
                            final byte b[] = new byte[len];
                            try {
                                result.getEntity().getContent().read(b);
                                myExec.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        processResponse(b);
                                    }
                                });
                                myExec.execute(lp.get());
                            } catch (Throwable e) {
                                log(e);
                                // delay next longpoll to avoid exception spam
                                delayed(lp.get(), 2000);
                            }
                        } else {
                            myExec.execute(lp.get());
                        }
                    }

                    @Override
                    public void failed(Exception ex) {
                        if (!timedout.compareAndSet(0, 1)) {
                            return;
                        }
                        log(ex);
                        // delay next longpoll to avoid exception spam
                        delayed(lp.get(), 2000);
                    }

                    @Override
                    public void cancelled() {
                        if (!timedout.compareAndSet(0, 1)) {
                            return;
                        }
                        log("request canceled");
                        // delay next longpoll to avoid exception spam
                        delayed(lp.get(), 2000);
                    }
                });
            }
        });
        delayed(lp.get(), 1000);
    }

    protected void checkTimeout() {
        if ( System.currentTimeMillis() - lastPing > timeout )
            disconnect("timed out");
    }

    protected static Timer timer = new Timer();

    /**
     * util to semd delayed jobs to processing/callback thread
     * @param runnable
     * @param millis
     */
    protected void delayed(final Runnable runnable, long millis) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    myExec.execute(runnable);
                } catch (Throwable t) {
                    log(t);
                }
            }
        }, millis);
    }

    protected void addRequest(RemoteCallEntry remoteCallEntry, Promise res) {
        if ( res != null ) {
            long key = registerCallback(res);
            remoteCallEntry.futureKey = key;
            openFutureRequests.incrementAndGet();
        }
        synchronized (requests) {
            requests.add(remoteCallEntry);
            sendRequests();
        }
    }

    public int getOpenFutureRequests() {
        return openFutureRequests.get();
    }

    /**
     * send an empty request polling for messages on server side. Unlike long poll,
     * this request returns immediately and is not delayed by the server
     */
    public void sendShortPoll() {
        myExec.execute(new Runnable() {
            @Override
            public void run() {
                Object[] req = new Object[] { "SP", lastSeenSeq };
                sendCallArray(req);
            }
        });
    }

    /**
     * sends pending requests async. needs be executed inside lock (see calls of this)
     */
    protected void sendRequests() {
        if ( ! requestUnderway ) {
            requestUnderway = true;
            delayed( new Runnable() {
                @Override
                public void run() {
                    synchronized (requests) {
                        Object req[];
                        if ( openFutureRequests.get() > 0 && requests.size() == 0 ) {
                            req = new Object[] { "SP", lastSeenSeq };
                        } else {
                            req = new Object[requests.size() + 1];
                            for (int i = 0; i < requests.size(); i++) {
                                req[i] = requests.get(i);
                            }
                            req[req.length - 1] = lastSeenSeq;
                            requests.clear();
                        }

                        sendCallArray(req).then(new Callback<Integer>() {
                            @Override
                            public void receive(Integer result, Object error) {
                                synchronized (requests ) {
                                    requestUnderway = false;
                                    if ( requests.size() > 0 || (result != null && result > 0 && openFutureRequests.get() > 0 ) ) {
                                        myExec.execute(new Runnable() {
                                            @Override
                                            public void run() {
                                            sendRequests();
                                            }
                                        });
                                    }
                                }
                            }
                        });
                    }
                }
            }, 1);
        }
    }

    /**
     * encodes amd semds (incl seqNo). needs be executed inside lock (see calls of this)
     */
    protected Promise<Integer> sendCallArray(Object[] req) {
        final Promise<Integer> p = new Promise<>();
        if (SENDDEBUG)
            System.out.println("SENDING "+(req.length-1));
        byte[] message = conf.asByteArray(req);
        HttpPost request = createRequest(sessionUrl, message);
        if ( DumpProtocol ) {
            try {
                System.out.println("req:"+new String(message,"UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        getClient().execute(request, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse result) {
                if (result.getStatusLine().getStatusCode() != 200) {
                    String error = "Unexpected status:" + result.getStatusLine().getStatusCode();
                    p.reject(error);
                    return;
                }
                lastPing = System.currentTimeMillis();
                String cl = result.getFirstHeader("Content-Length").getValue();
                int len = Integer.parseInt(cl);
                if (len > 0) {
                    final byte b[] = new byte[len];
                    try {
                        result.getEntity().getContent().read(b);
                        myExec.execute(new Runnable() {
                            @Override
                            public void run() {
                                int numMsgResp = 0;
                                try {
                                    numMsgResp = processResponse(b);
                                } finally {
                                    p.complete(numMsgResp,null);
                                }
                            }
                        });
                    } catch (Throwable e) {
                        p.complete(null,e);
                        log(e);
                    }
                } else {
                    p.complete(0,null);
                }
            }

            @Override
            public void failed(Exception ex) {
                p.complete(null,ex);
                log(ex);
            }

            @Override
            public void cancelled() {
                p.complete(0,null);
                log("request canceled");
            }
        });
        return p;
    }

    Map sequenceCache = new HashMap(); // caches early responses in case of response race
    protected int processResponse(byte[] b) {
        if ( DumpProtocol ) {
            try {
                System.out.println("resp:"+new String(b,"UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        Object o[] = (Object[]) conf.asObject(b);
        if ( SENDDEBUG )
            System.out.println("RECEIVE:"+(o.length-1));
        int seq = ((Number) o[o.length - 1]).intValue();
        if ( seq == lastSeenSeq+1 || lastSeenSeq == 0 ) {
            lastSeenSeq = seq;
            processDecodedResultArray(o);
            Object next[];
            while ( (next = (Object[]) sequenceCache.get(lastSeenSeq + 1)) != null ) {
                lastSeenSeq++;
                sequenceCache.remove(lastSeenSeq);
                log("replay "+lastSeenSeq);
                processDecodedResultArray(next);
            }
        } else {
            log("ignored result with sequence:" + seq + " lastSeen:" + lastSeenSeq);
            if ( seq > lastSeenSeq ) {
                sequenceCache.put(seq,o);
                if ( sequenceCache.size() > 5 ) {
                    disconnect("Unrecoverable Gap");
                }
            }
        }
        return o.length-1;
    }

    public void close() {
        disconnect("closed");
    }

    protected void disconnect(String s) {
        isConnected = false;
        if ( connectionListener != null ) {
            connectionListener.connectionClosed(s);
        }
        lastSeenSeq = 0;
        myExec.shutdown();
    }

    protected void processDecodedResultArray(Object[] o) {
        for (int i = 0; i < o.length-1; i++) {
            final RemoteCallEntry call = (RemoteCallEntry) o[i];
            if ( call.getQueue() == 1 ) // callback
            {
                // catch and transform remote actor reference
                if ( call.getArgs()[0] instanceof Unknown) {
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
                final Callback callback = callbackMap.get(call.getReceiverKey());
                if ( callback == null ) {
                    log("unknown callback receiver "+call);
                } else {
                    final Object error = call.getArgs()[1];
                    if ( callback instanceof Promise) {
                        openFutureRequests.decrementAndGet();
                        callbackMap.remove(call.getReceiverKey());
                    }
                    else {
                        if ( ! "CNT".equals(error) ) {
                            callbackMap.remove(call.getReceiverKey());
                        }
                    }
                    myExec.execute(new Runnable() {
                        @Override
                        public void run() {
                            callback.receive(call.getArgs()[0], error);
                        }
                    });
                }
            }
        }
    }

    protected long registerCallback(Callback res) {
        long key = idCount.incrementAndGet();
        callbackMap.put(key, res );
        return key;
    }

    protected void log(Throwable e) {
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

}
