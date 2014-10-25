package org.nustaq.kontraktor.remoting.http.netty.wsocket;

import io.netty.channel.ChannelHandlerContext;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.impl.RemoteScheduler;
import org.nustaq.kontraktor.remoting.Coding;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.remoting.SerializerType;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.netty2go.WebSocketClient;
import org.nustaq.serialization.FSTConfiguration;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by ruedi on 30.08.14.
 *
 * Actor Connection via WebSockets and (defaults to MinBin encoding)
 *
 */
public class WSocketActorClient<T extends Actor> extends RemoteRefRegistry {

    public static <T extends Actor> Future<T> Connect( Class<T> clz, String host, int port ) throws IOException {
        Promise<T> res = new Promise<>();
        WSocketActorClient<T> client = new WSocketActorClient<>( clz, host, new Coding(SerializerType.MinBin));
        new Thread(() -> {
            try {
                client.connect();
                res.receive(client.getFacadeProxy(), null);
            } catch (IOException e) {
                Log.Warn(WSocketActorClient.class, e, "");
                res.receive(null, e);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "connect "+client.getDescriptionString()).start();
        return res;
    }

    Class<? extends Actor> actorClazz;
    T facadeProxy;

    String url;
    WSActorClient client;
    MyWSObjectSocket socket;

    int maxTrialConnect = 60; // number of trials on initial connect (each second)

    public WSocketActorClient(Class<? extends Actor> clz, String url, Coding code) throws IOException {
	    super(code);
        this.url = url;
        actorClazz = clz;
        facadeProxy = Actors.AsActor(actorClazz, new RemoteScheduler());
        facadeProxy.__remoteId = 1;
        registerRemoteRefDirect(facadeProxy);
    }

    public T getFacadeProxy() {
        return facadeProxy;
    }

    public int getMaxTrialConnect() {
        return maxTrialConnect;
    }

    public void setMaxTrialConnect(int maxTrialConnect) {
        this.maxTrialConnect = maxTrialConnect;
    }

    public void connect() throws Exception {
        int count = 0;
        while (count < maxTrialConnect ) {
            try {
                client = new WSActorClient();
                client.connect(url);
                if ( client.isConnected() ) {
                    facadeProxy.__addRemoteConnection(client);
                    break;
                }
                Thread.sleep(1000);
            } catch (Exception ex) {
                count++;
                Log.Info(this,"connection to " + getDescriptionString() + " failed, retry " + count + " of " + maxTrialConnect);
                if ( count >= maxTrialConnect ) {
                    Log.Lg.error(this,ex,"connection failed. giving up");
                    throw ex;
                }
            }
        }
        if ( isConnected() ) {
            socket = new MyWSObjectSocket(conf);
            new Thread(()->{
                try {
                    currentObjectSocket.set(socket);
                    sendLoop(socket);
                } catch (IOException e) {
                    close();
                }
            },"wsclient sender").start();
        }
    }

    private String getDescriptionString() {
        return actorClazz.getSimpleName() + "@" + url;
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    /**
     *
     */
    class WSActorClient extends WebSocketClient implements RemoteConnection {

        public WSActorClient() throws IOException {
            super();
        }

        @Override
        public void onClose(ChannelHandlerContext ctx) {
            super.onClose(ctx);
            WSocketActorClient.this.close();
        }

        @Override
        public void onTextMessage(ChannelHandlerContext ctx, String text) {
            System.out.println("unexpected text message:"+text);
        }

        @Override
        public void onBinaryMessage(ChannelHandlerContext ctx, byte[] buffer) {
            try {
                currentObjectSocket.set(socket);
                socket.setNextMsg(buffer);
                singleReceive(socket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onPing(ChannelHandlerContext ctx) {
            super.onPing(ctx);
        }

        @Override
        public void onPong(ChannelHandlerContext ctx) {
            super.onPong(ctx);
        }
    }

    @Override
    protected void remoteRefStopped(Actor actor) {
        super.remoteRefStopped(actor);
        if (actor.getActorRef() == facadeProxy.getActorRef() ) {
            // connection closed => close connection and stop all remoteRefs
            setTerminated(true);
            stopRemoteRefs();
            client.close();
        }
    }

    private class MyWSObjectSocket extends WSAbstractObjectSocket {

        /**
         * its expected conf has special registrations such as Callback and remoteactor ref
         *
         * @param conf
         */
        public MyWSObjectSocket(FSTConfiguration conf) {
            super(conf);
        }

        @Override
        public void writeObject(Object toWrite) throws Exception {
            synchronized (this) { // FIXME: required ?
                byte[] b = conf.asByteArray((Serializable) toWrite);
                client.sendBinary(b, 0, b.length);
            }
        }

        @Override
        public void flush() throws IOException {
            client.flush();
        }

        @Override
        public void setLastError(Exception ex) {

        }

        @Override
        public void close() throws IOException {
            client.close();
        }
    }

}
