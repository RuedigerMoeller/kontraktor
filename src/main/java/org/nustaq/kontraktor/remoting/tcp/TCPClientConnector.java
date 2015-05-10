package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.*;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.net.TCPObjectSocket;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Created by ruedi on 10/05/15.
 */
public class TCPClientConnector implements ActorClientConnector {


    public static <T extends Actor> IPromise<T> Connect( Class<? extends Actor<T>> clz, String host, int port, Coding c ) {
        Promise result = new Promise();
        Runnable connect = () -> {
            TCPClientConnector client = new TCPClientConnector(port,host);
            ActorClient connector = new ActorClient(client,clz,c);
            connector.connect().then(result);
        };
        if ( ! Actor.inside() ) {
            get().execute(() -> Thread.currentThread().setName("singleton remote client actor polling"));
            get().execute(connect);
        }
        else
            connect.run();
        return result;
    }

    public static class RemotingHelper extends Actor<RemotingHelper> {}
    static AtomicReference<RemotingHelper> singleton =  new AtomicReference<>();

    /**
     * in case clients are connected from non actor world, provide a global actor(thread) for remote client processing
     * (=polling queues, encoding)
     */
    static RemotingHelper get() {
        synchronized (singleton) {
            if ( singleton.get() == null ) {
                singleton.set(Actors.AsActor(RemotingHelper.class));
            }
            return singleton.get();
        }
    }

    int port;
    String host;
    MyTCPSocket socket;

    public TCPClientConnector(int port, String host) {
        this.port = port;
        this.host = host;
    }

    @Override
    public void connect(Function<ObjectSocket, ObjectSink> factory) throws Exception {
        socket = new MyTCPSocket(host,port);
        ObjectSink sink = factory.apply(socket);
        new Thread(() -> {
            while (!socket.isClosed()) {
                try {
                    Object o = socket.readObject();
                    sink.receiveObject(o);
                } catch (Exception e) {
                    if (e instanceof EOFException == false)
                        Log.Warn(this, e);
                }
            }
            sink.sinkClosed();
        }, "tcp client receiver").start();
    }

    @Override
    public IPromise closeClient() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            return new Promise<>(e);
        }
        return new Promise<>();
    }

    static class MyTCPSocket extends TCPObjectSocket implements ObjectSocket {

        public MyTCPSocket(String host, int port) throws IOException {
            super(host, port);
        }
    }

}
