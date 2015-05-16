package org.nustaq.kontraktor.remoting.http;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.ParseException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
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
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Created by ruedi on 13/05/15.
 *
 * uses sync http as i want to avoid fat dependencies for async http. should be ok for client side
 *
 * Long Poll, http 1.1 based connectivity with on top sequencing + reordering. Designed to handle network outages
 * and crappy connectivity (e.g. mobile's).
 * This implies once it successfully connected, it won't go to "closed" except a session timeout/close happened (=>404) (even if server has been stopped after connection)
 *
 * TODO: proxy options
 * TODO: provide 1.7 compliant Android client impl
 * TODO: http auth
 * TODO: internal auth (impl is there, expose)
 */
public class HttpClientConnector implements ActorClientConnector {

    public static int LP_TIMEOUT = 20000;

    public static <T extends Actor> IPromise<T> Connect( Class<? extends Actor<T>> clz, String url, Callback<ActorClientConnector> disconnectCallback, Coding c ) {
        HttpClientConnector con = new HttpClientConnector(url);
        con.disconnectCallback = disconnectCallback;
        ActorClient actorClient = new ActorClient(con, clz, c == null ? new Coding(SerializerType.FSTSer) : c);
        Promise p = new Promise();
        getClientHelperActor().execute( () -> actorClient.connect().then(p) );
        return p;
    }

    String host;
    String sessionId;
    FSTConfiguration authConf = FSTConfiguration.createMinBinConfiguration();
    volatile boolean isClosed = false;
    Promise closedNotification;
    Callback<ActorClientConnector> disconnectCallback;

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

    protected void startLongPoll(MyHttpWS myHttpWS) {
        // start longpoll
        Runnable lp[] = {null};
        lp[0] = () -> {
            // sends either queued outgoing calls or creates LP request
            myHttpWS.asyncFlush().then((r, e) -> {
                if (isClosed) {
                    if ( closedNotification != null ) {
                        closedNotification.complete();
                        closedNotification = null;
                    }
                    return;
                }
                if ( r != null )
                    Actor.current().execute(lp[0]);
                else // error
                    Actor.current().delayed(1000, lp[0]);
            });
        };
        Actor.current().execute( lp[0] );
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

        public MyHttpWS(String url) {
            this.url = url;
        }

        @Override
        public void sendBinary(byte[] message) {
            // valid as in http executed synchronized after sequence inc
            asyncSend(sendSequence.get(),message);
        }

        IPromise asyncSend(int msgSequence, byte[] message) {
            Promise p = new Promise();
            int seq = lastReceivedSequence;
            Actor.current().$exec( () -> {
                try {
                    Content content = Request.Post(url+"/"+seq)
                        .socketTimeout(LP_TIMEOUT)
                        .addHeader(NO_CACHE)
                        .bodyByteArray(message)
                        .execute()
                        .returnContent();
                    return content;
                } catch (IOException e) {
                    Actors.throwException(e);
                }
                return null;
            }).then((content, ex) -> {
//                boolean drop = Math.random() > .9;
//                if (drop)
//                    System.out.println("drop!");
                boolean drop = false;

                if (content != null && ! drop ) {
                    byte[] b = ((Content) content).asBytes();
                    if (b.length > 0) {
                        Object o = getConf().asObject(b);
                        boolean send = true;
                        if ( o instanceof Object[] ) {
                            Object ar[] = (Object[]) o;
                            int sequence = ((Number) ar[ar.length - 1]).intValue();
                            if ( lastReceivedSequence > 0 ) {
                                send = lastReceivedSequence == sequence - 1;
                            }
                            if (send)
                                lastReceivedSequence = sequence;
                        }
                        if (send)
                            sink.receiveObject(o);
                    }
                } else {
                    if ( ex instanceof HttpResponseException && ((HttpResponseException)ex).getStatusCode() == 404 ) {
                        closeClient();
                    } else if ( ex != null )
                        ((Throwable) ex).printStackTrace();
                    // todo: resend
                }
                p.complete(content, ex);
            });
            return p;
        }

        @Override
        public synchronized void writeObject(Object toWrite) throws Exception {
            super.writeObject(toWrite);
        }

        @Override
        public synchronized void flush() throws Exception {
            if ( objects.size() == 0 )
                return;
            asyncFlush();
        }

        public synchronized IPromise asyncFlush() {
            int seq = sendSequence.incrementAndGet();
            objects.add(seq); // sequence
            Object[] objArr = objects.toArray();
            objects.clear();
            return asyncSend(seq,conf.asByteArray(objArr));
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
     * helper actor to run long polling related tasks on
     */
    static HttpClientActor singleton = Actors.AsActor(HttpClientActor.class);
    public static HttpClientActor getClientHelperActor() {
        synchronized (HttpClientConnector.class) {
            if (singleton == null )
                singleton = Actors.AsActor(HttpClientActor.class);
            return singleton;
        }
    }

    public static void main( String a[] ) throws InterruptedException {

        try {
            for (int i = 0; i < 1; i++ ) {

                UndertowHttpConnector.HTTPA act
                    = Connect(
                        UndertowHttpConnector.HTTPA.class,
                        "http://localhost:8080/http",
                        (res, err) -> System.out.println("closed"),
                        new Coding(SerializerType.MinBin)
                    ).await();

                int count[] = {0};
                Runnable pok[] = {null};
                pok[0] = () -> {
                    act.hello("pok").then(r -> {
                        System.out.println("response:" + count[0]++ + " " + r);
        //                        if (count[0] - 1 != ((Number) r).intValue())
        //                            System.exit(0);
                        Actors.SubmitDelayed(100, pok[0]);
                    });
                };
                Actors.SubmitDelayed(1, pok[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Thread.sleep(100000000);
    }

}
