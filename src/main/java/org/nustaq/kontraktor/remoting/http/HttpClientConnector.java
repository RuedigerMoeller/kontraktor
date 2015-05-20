package org.nustaq.kontraktor.remoting.http;

import org.apache.http.*;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.base.ActorClient;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.base.ObjectSink;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.WebObjectSocket;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.FSTConfiguration;

import java.io.*;
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
 * TODO: http auth
 * TODO: internal auth (impl is there, expose)
 */
public class HttpClientConnector implements ActorClientConnector {

    public static <T extends Actor> IPromise<T> Connect( Class<? extends Actor<T>> clz, String url, Callback<ActorClientConnector> disconnectCallback, Coding c) {
        return Connect(clz,url,disconnectCallback,c,LONG_POLL);
    }

    public static <T extends Actor> IPromise<T> Connect( Class<? extends Actor<T>> clz, String url, Callback<ActorClientConnector> disconnectCallback, Coding c, HttpClientConfig cfg ) {
        HttpClientConnector con = new HttpClientConnector(url);

        con.cfg = cfg;
        con.disconnectCallback = disconnectCallback;
        ActorClient actorClient = new ActorClient(con, clz, c == null ? new Coding(SerializerType.FSTSer) : c);
        Promise p = new Promise();
        getRefPollActor().execute(() -> {
            Thread.currentThread().setName("Http Ref Polling");
            actorClient.connect().then(p);
        });
        return p;
    }

    public static HttpClientConfig LONG_POLL = new HttpClientConfig().noPoll(false);
    public static HttpClientConfig SHORT_POLL = new HttpClientConfig().noPoll(false).shortPoll(true);
    public static HttpClientConfig NO_POLL = new HttpClientConfig().noPoll(true);

    public static class HttpClientConfig {
        public boolean noPoll = true;
        public boolean shortPollMode = false;   // if true, do short polling instead
        public long shortPollIntervalMS = 5000;

        public HttpClientConfig noPoll(boolean noPoll) {
            this.noPoll = noPoll;
            return this;
        }

        public HttpClientConfig shortPoll(boolean shortPollMode) {
            this.shortPollMode = shortPollMode;
            return this;
        }

        public HttpClientConfig shortPollIntervalMS(long shortPollIntervalMS) {
            this.shortPollIntervalMS = shortPollIntervalMS;
            return this;
        }
    }

    String host;
    String sessionId;
    FSTConfiguration authConf = FSTConfiguration.createMinBinConfiguration();
    volatile boolean isClosed = false;
    Promise closedNotification;
    Callback<ActorClientConnector> disconnectCallback;
    HttpClientConfig cfg = new HttpClientConfig();

    long currentShortPollIntervalMS = cfg.shortPollIntervalMS;

    public HttpClientConnector(String host) {
        this.host = host;
    }

    @Override
    public void connect(Function<ObjectSocket, ObjectSink> factory) throws Exception {
        Content content = Request.Post(host)
                              .bodyByteArray(authConf.asByteArray(new Object[]{"authentication", "data"}))
                              .execute()
                              .returnContent();
        sessionId = (String) authConf.asObject(content.asBytes());
        MyHttpWS myHttpWS = new MyHttpWS(host + "/" + sessionId);
        ObjectSink sink = factory.apply(myHttpWS);
        myHttpWS.setSink(sink);
        startLongPoll(myHttpWS);
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
                myHttpWS.longPoll().then((r, e) -> {
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
        CloseableHttpAsyncClient asyncHttpClient;
        CloseableHttpAsyncClient lpHttpClient;
        AtomicInteger openRequests = new AtomicInteger(0);

        public MyHttpWS(String url) {
            this.url = url;
            asyncHttpClient = HttpAsyncClients.custom().setDefaultIOReactorConfig( IOReactorConfig.custom().setIoThreadCount(1).setSoKeepAlive(true).build() ).build();
            asyncHttpClient.start();
            if ( ! cfg.shortPollMode ) {
                lpHttpClient = HttpAsyncClients.custom().setDefaultIOReactorConfig( IOReactorConfig.custom().setIoThreadCount(1).setSoKeepAlive(true).build() ).build();
                lpHttpClient.start();
            }
        }

        @Override
        public void sendBinary(byte[] message) {
            openRequests.incrementAndGet();
            HttpPost req = new HttpPost(url+"/"+lastReceivedSequence);
            req.addHeader(NO_CACHE);
            req.setEntity(new ByteArrayEntity(message));
            asyncHttpClient.execute(req, new FutureCallback<HttpResponse>() {
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
//                                    getRefPollActor().execute( () -> sink.receiveObject(o) );
                            sink.receiveObject(o, null);
                        }
                        else {
//                            Log.Warn(this, "IGNORED LP RESPONSE, OUT OF SEQ");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                p.resolve();
            };
        }

        @Override
        public void writeObject(Object toWrite) throws Exception {
            if ( ! "SP".equals(toWrite) ) { // short polling marker
                // decrease poll interval temporary (backoff)
                currentShortPollIntervalMS = 200;
                // trigger next poll in 100 ms
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

    public static void main( String a[] ) throws InterruptedException {

        try {
            for (int i = 0; i < 1; i++ ) {

                UndertowHttpServerConnector.HTTPA act
                    = Connect(
                        UndertowHttpServerConnector.HTTPA.class,
                        "http://localhost:8080/myservice",
                        (res, err) -> System.out.println("closed"),
                        null, //new Coding(SerializerType.MinBin)
                        NO_POLL
                    ).await();

                int count[] = {0};
                Runnable pok[] = {null};
                pok[0] = () -> {
                    act.$ui();
                    act.hello("pok").then( r -> {
                        System.out.println("response:" + count[0]++ + " " + r);
                    });
                    Actors.SubmitDelayed((long) (Math.random()*1000), pok[0]);
                };
//                pok[0] = () -> {
//                    System.out.println("Call => ");
//                    act.$cb( (r,e) -> {
//                        System.out.println("pok "+r);
//                    });
//                    Actors.SubmitDelayed((long) (Math.random()*10000), pok[0]);
//                };
                Actors.SubmitDelayed(1, pok[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Thread.sleep(100000000);
    }

}
