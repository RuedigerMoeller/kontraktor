package org.nustaq.kontraktor.remoting.http;

import org.apache.http.*;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.kontraktor.remoting.websockets.WebObjectSocket;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.FSTConfiguration;

import java.io.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Created by ruedi on 13/05/15.
 *
 * Long Poll, http 1.1 based connectivity with on top sequencing + reordering. Designed to handle network outages
 * and crappy connectivity.
 * This implies once it successfully connected, it won't go to "closed" except a session timeout/close happened (=>404) (even if server has been stopped after connection)
 *
 * TODO: proxy options
 * TODO: provide 1.7 compliant Android client impl
 */
public class HttpClientConnector implements ActorClientConnector {

    public static boolean DumpProtocol = false;

    protected static CloseableHttpAsyncClient asyncHttpClient;
    public static CloseableHttpAsyncClient getClient() {
        synchronized (HttpClientConnector.class) {
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

    String sessionId;
    FSTConfiguration authConf = FSTConfiguration.createJsonConfiguration();
    volatile boolean isClosed = false;
    Promise closedNotification;
    Callback<ActorClientConnector> disconnectCallback;
    HttpConnectable cfg;

    long currentShortPollIntervalMS;
    public Object[] authData;

    public HttpClientConnector(HttpConnectable httpConnectable) {
        this.cfg = httpConnectable;
        currentShortPollIntervalMS = cfg.getShortPollIntervalMS();
    }

    @Override
    public IPromise connect(Function<ObjectSocket, ObjectSink> factory) throws Exception {
        Promise res = new Promise();

        byte[] message = authConf.asByteArray(authData);
        if ( HttpClientConnector.DumpProtocol ) {
            try {
                System.out.println("auth-req:"+new String(message,"UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        HttpPost req = new HttpPost(cfg.getUrl());
        req.addHeader(NO_CACHE);
        req.setEntity(new ByteArrayEntity(message));
        Executor actor = Actor.current();
        getClient().execute(req, new FutureCallback<HttpResponse>() {
            @Override
            public void completed(HttpResponse result) {
                if (result.getStatusLine().getStatusCode() != 200) {
                    closeClient();
                    res.reject(result.getStatusLine().getStatusCode());
                    return;
                }
                String cl = result.getFirstHeader("Content-Length").getValue();
                int len = Integer.parseInt(cl);
                if (len > 0) {
                    byte resp[] = new byte[len];
                    try {
                        result.getEntity().getContent().read(resp);
                        if (HttpClientConnector.DumpProtocol) {
                            try {
                                System.out.println("auth-resp:" + new String(resp, "UTF-8"));
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                        actor.execute( () -> {
                            sessionId = (String) authConf.asObject(resp);
                            MyHttpWS myHttpWS = new MyHttpWS(cfg.getUrl() + "/" + sessionId);
                            ObjectSink sink = factory.apply(myHttpWS);
                            myHttpWS.setSink(sink);
                            startLongPoll(myHttpWS);
                            res.resolve();
                        });
                    } catch (Exception e) {
                        Log.Warn(this, e);
                        res.reject(e);
                    }
                } else {
                    res.reject("connection rejected, no connection id");
                }
            }

            @Override
            public void failed(Exception ex) {
                res.reject(ex);
            }

            @Override
            public void cancelled() {
                res.reject("canceled");
            }
        });
        return res;
    }

    Runnable pollRunnable;
    protected void startLongPoll(MyHttpWS myHttpWS) {
        if ( cfg.noPoll )
            return;
        // start longpoll/shortpoll
        currentShortPollIntervalMS = cfg.shortPollIntervalMS;
        pollRunnable = () -> {
            if ( cfg.shortPollMode ) {
                getRefPollActor().delayed(currentShortPollIntervalMS, () -> {
                    if (isClosed) {
                        if (closedNotification != null) {
                            closedNotification.complete();
                            closedNotification = null;
                        }
                        return;
                    }
                    try {
                        myHttpWS.writeObject("SP");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Actor.current().delayed(currentShortPollIntervalMS, pollRunnable);
                    if (currentShortPollIntervalMS < cfg.shortPollIntervalMS) {
                        currentShortPollIntervalMS *= 2;
                        currentShortPollIntervalMS = Math.min(cfg.shortPollIntervalMS, currentShortPollIntervalMS);
                    }
                });
            } else {
                // sends either queued outgoing calls or creates LP request
                myHttpWS.longPoll().then( (r, e) -> {
                    if (isClosed) {
                        if (closedNotification != null) {
                            closedNotification.complete();
                            closedNotification = null;
                        }
                        return;
                    }
                    if (e == null)
                        Actor.current().execute(pollRunnable);
                    else // error
                        Actor.current().delayed(1000, pollRunnable);
                });
            }
        };
        if ( ! cfg.shortPollMode ) {
            getReceiveActor().__currentDispatcher.setName("Http LP dispatcher");
            getReceiveActor().execute(pollRunnable);
        } else {
            getRefPollActor().execute(pollRunnable);
        }
    }

    @Override
    public IPromise closeClient() {
        closedNotification = new Promise();
        isClosed = true;
        if (disconnectCallback!=null)
            disconnectCallback.complete(this,null);
        Log.Info(this,"connection closing");
        return closedNotification;
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

    class MyHttpWS extends WebObjectSocket {

        String url;
        ObjectSink sink;
        int lastReceivedSequence = 0;
        CloseableHttpAsyncClient lpHttpClient;
        AtomicInteger openRequests = new AtomicInteger(0);

        public MyHttpWS(String url) {
            this.url = url;
            if ( ! cfg.shortPollMode ) {
                lpHttpClient = HttpAsyncClients.custom().setDefaultIOReactorConfig( IOReactorConfig.custom().setIoThreadCount(1).setSoKeepAlive(true).build() ).build();
                lpHttpClient.start();
            }
        }

        @Override
        public void sendBinary(byte[] message) {
            if ( HttpClientConnector.DumpProtocol ) {
                try {
                    System.out.println("req:"+new String(message,"UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            openRequests.incrementAndGet();
            HttpPost req = new HttpPost(url+"/"+lastReceivedSequence);
            req.addHeader(NO_CACHE);
            req.setEntity(new ByteArrayEntity(message));
            getClient().execute(req, new FutureCallback<HttpResponse>() {
                @Override
                public void completed(HttpResponse result) {
                    openRequests.decrementAndGet();
                    Runnable processLPResponse = getProcessLPRunnable(new Promise(), result);
                    getReceiveActor().execute(processLPResponse);
                    getRefPollActor().execute(() -> {
                        try {
                            flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }

                @Override
                public void failed(Exception ex) {
                    // FIXME: resend
                    ex.printStackTrace();
                    openRequests.decrementAndGet();
                }

                @Override
                public void cancelled() {
                    Log.Warn(this, "request cancelled");
                    openRequests.decrementAndGet();
                }
            });
        }

        IPromise longPollSend(byte[] message) {
            Promise p = new Promise();
            int seq = lastReceivedSequence;

            HttpPost req = new HttpPost(url+"/"+seq);
            req.addHeader(NO_CACHE);
            req.setEntity(new ByteArrayEntity(message));
            if ( HttpClientConnector.DumpProtocol ) {
                try {
                    System.out.println("req:"+new String(message,"UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            lpHttpClient.execute(req, new FutureCallback<HttpResponse>() {
                @Override
                public void completed(HttpResponse result) {
                    Runnable processLPRespponse = getProcessLPRunnable(p,result);
                    getReceiveActor().execute(processLPRespponse);
                }

                @Override
                public void failed(Exception ex) {
                    // FIXME: resend
                    ex.printStackTrace();
                    p.reject(ex);
                }

                @Override
                public void cancelled() {
                    System.out.println("cancel");
                    p.reject("Canceled");
                }
            });
            return p;
        }

        protected Runnable getProcessLPRunnable(Promise p, HttpResponse result) {
            return () -> {
                if (result.getStatusLine().getStatusCode() == 404) {
                    closeClient();
                    p.reject("Closed");
                    return;
                }
                String cl = result.getFirstHeader("Content-Length").getValue();
                int len = Integer.parseInt(cl);
                if (len > 0) {
                    byte b[] = new byte[len];
                    try {
                        result.getEntity().getContent().read(b);

                        if ( HttpClientConnector.DumpProtocol ) {
                            try {
                                System.out.println("resp:"+new String(b,"UTF-8"));
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }

                        Object o = getConf().asObject(b);
                        boolean send = true;
                        if (o instanceof Object[]) {
                            Object ar[] = (Object[]) o;
                            int sequence = ((Number) ar[ar.length - 1]).intValue();
                            if (lastReceivedSequence > 0) {
                                send = lastReceivedSequence == sequence - 1;
                            }
                            if (send)
                                lastReceivedSequence = sequence;
                        }
                        if (send) {
                            sink.receiveObject(o, null);
                        }
                        else {
//                            Log.Warn(this, "IGNORED LP RESPONSE, OUT OF SEQ");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if ( HttpClientConnector.DumpProtocol ) {
                    System.out.println("resp:<EMPTY>");
                }

                p.resolve();
            };
        }

        @Override
        public void writeObject(Object toWrite) throws Exception {
            if ( ! "SP".equals(toWrite) ) { // short polling marker
                // fixme: should check if short polling is enabled !
                // a call is made
                // decrease poll interval temporary (backoff)
                currentShortPollIntervalMS = 200;
                // trigger a short poll in 100 ms
                getRefPollActor().delayed(100, () -> {
                    try {
                        writeObject("SP");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            objects.add(toWrite);
            if (objects.size() > getObjectMaxBatchSize()) {
                flush();
            }
        }

        @Override
        public boolean canWrite() {
            return openRequests.get() == 0 || objects.size() < getObjectMaxBatchSize();
        }

        @Override
        public void flush() throws Exception {
            if (openRequests.get() == 0)
                super.flush();
        }

        @Override
        protected int getObjectMaxBatchSize() {
            // huge batch size to make up for stupid sync http 1.1 protocol enforcing latency inclusion
            return HttpObjectSocket.HTTP_BATCH_SIZE;
        }

        public IPromise longPoll() {
            int seq = sendSequence.incrementAndGet();
            Object[] objArr = {seq};
            return longPollSend(conf.asByteArray(objArr));
        }

        @Override
        public void close() throws IOException {
            closeClient();
        }

        public void setSink(ObjectSink sink) {
            this.sink = sink;
        }

        public ObjectSink getSink() {
            return sink;
        }
    }

    public static class HttpClientActor extends Actor<HttpClientActor> {
    }

    /**
     * helper actor receive side
     */
    static HttpClientActor singletonRec = Actors.AsActor(HttpClientActor.class);
    public static HttpClientActor getReceiveActor() {
        synchronized (HttpClientConnector.class) {
            if (singletonRec == null )
                singletonRec = Actors.AsActor(HttpClientActor.class);
            return singletonRec;
        }
    }

    /**
     * helper actor send side
     */
    static HttpClientActor singletonRefPoll = Actors.AsActor(HttpClientActor.class);
    public static HttpClientActor getRefPollActor() {
        synchronized (HttpClientConnector.class) {
            if (singletonRefPoll == null )
                singletonRefPoll = Actors.AsActor(HttpClientActor.class);
            return singletonRefPoll;
        }
    }

}
