package org.nustaq.kontraktor.remoting.http;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.ParseException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
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
import java.util.function.Function;

/**
 * Created by ruedi on 13/05/15.
 * <p>
 * uses sync http as i want to avoid fat dependencies for async http. should be ok for client side
 *
 */
public class HttpClientConnector implements ActorClientConnector {

    public static int LP_TIMEOUT = 50000;

    String host;
    String sessionId;
    FSTConfiguration authConf = FSTConfiguration.createMinBinConfiguration();
    volatile boolean isClosed = false;
    Runnable disconnectCallback;

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
        return null;
    }

    class MyHttpWS extends WebObjectSocket {

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
                boolean drop = Math.random() > .9;
                if (drop)
                    System.out.println("drop!");

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
                        close();
                    } else
                    if ( ex != null )
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
            isClosed = true;
            if (disconnectCallback!=null)
                disconnectCallback.run();
            Log.Info(this,"connection closed");
        }

        public void setSink(ObjectSink sink) {
            this.sink = sink;
        }

        public ObjectSink getSink() {
            return sink;
        }
    }

    public static class DummyA extends Actor<DummyA> {

    }

    public static void main( String a[] ) throws InterruptedException {

        for (int i = 0; i < 1; i++ ) {
            DummyA da = Actors.AsActor(DummyA.class);

            HttpClientConnector con = new HttpClientConnector("http://localhost:8080/http");
            ActorClient actorClient = new ActorClient(con, UndertowHttpConnector.HTTPA.class, new Coding(SerializerType.MinBin));
            da.execute(() -> {
                UndertowHttpConnector.HTTPA act = (UndertowHttpConnector.HTTPA) actorClient.connect().await();
                int count[] = {0};Runnable pok[] = {null};
                pok[0] = () -> {
                    act.hello("pok").then(r -> {
                        System.out.println("response:" + count[0]++ + " " + r);
                        if (count[0] - 1 != ((Number) r).intValue())
                            System.exit(0);
                        da.delayed(1, pok[0]);
                    });
                };
                da.delayed(1, pok[0]);
            });
        }
        Thread.sleep(100000000);
    }

}
