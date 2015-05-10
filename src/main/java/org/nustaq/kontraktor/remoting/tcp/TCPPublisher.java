package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.base.RemoteRegistry;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.kontraktor.remoting.base.RemoteRefPolling;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.net.TCPObjectSocket;
import org.nustaq.serialization.FSTConfiguration;

import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Created by ruedi on 09/05/15.
 */
public class TCPPublisher {


    public static TCPPublisher Publish(Actor act, int port ) throws Exception {
        return Publish(act,port,null);
    }

    public static TCPPublisher Publish(Actor act, int port, Coding coding ) throws Exception {
        TCPPublisher tcpapNew = new TCPPublisher(act, port, coding);
        tcpapNew.start();
        return tcpapNew;
    }

    static TCPSendLoop tcpActorPolling = Actors.AsActor(TCPSendLoop.class,100_000);

    public static class TCPSendLoop extends Actor<TCPSendLoop> {

        RemoteRefPolling polling;

        public IPromise $schedule( RemoteRegistry reg ) {
            if ( polling == null ) {
                polling = new RemoteRefPolling();
                Thread.currentThread().setName("syncTCP actor polling");
            }
            return polling.scheduleSendLoop(reg);
        }

    }

    protected int port;
    protected ServerSocket acceptSocket;
    protected Actor facade;
    protected Consumer<Actor> closeListener;
    protected Coding coding;

    public TCPPublisher(Actor proxy, int port, Coding coding) throws Exception {
        this.port = port;
        this.coding = coding;
        this.facade = proxy;
        if (this.coding == null) {
            this.coding = new Coding(SerializerType.FSTSer);
        }
    }

    public void start() throws Exception {
        new Thread( () -> acceptLoop(), "acceptor thread "+port).start();
    }

    protected void acceptLoop() {
        try {
            connectServerSocket();
            while (!acceptSocket.isClosed()) {

                Socket clientSocket = acceptSocket.accept();

                // setup registry
                AtomicReference<ObjectSocket> ref = new AtomicReference<>();
                RemoteRegistry reg = new RemoteRegistry(coding) {
                    @Override
                    public Actor getFacadeProxy() {
                        return facade;
                    }
                    @Override
                    public AtomicReference<ObjectSocket> getWriteObjectSocket() {
                        return ref;
                    }
                };
                ref.set(new ClientObjectSocket(clientSocket, reg.getConf()));

                tcpActorPolling.$schedule(reg).then((r, e) -> {
                    System.out.println("actor unscheduled " + r + " " + e);
                });

                new Thread( () -> {
                    while( ! clientSocket.isClosed() ) {
                        try {
                            Object o = ((ClientObjectSocket) ref.get()).readObject();
                            reg.receiveObject(ref.get(),o);
                        } catch (Exception e) {
                            if ( e instanceof EOFException == false )
                                Log.Warn(this,e);
                        }
                    }
                    reg.cleanUp();
                    reg.setTerminated(true);

                }, "receiver ").start();
                reg.publishActor(facade);
            }
        } catch (Exception e) {
            Log.Info(this, e);
        } finally {
            try {
                acceptSocket.close();
            } catch (IOException e) {
                Log.Warn(this,e);
            }
        }
    }

    public IPromise close() {
        try {
            acceptSocket.close();
        } catch (IOException e) {
            return new Promise<>(null,e);
        }
        return new Promise<>("done");
    }

    public Actor getFacade() {
        return facade;
    }

    protected void connectServerSocket() throws Exception {
        acceptSocket = new ServerSocket(port);
        Log.Info(this, facade.getActor().getClass().getName() + " running on " + acceptSocket.getLocalPort());
    }

    class ClientObjectSocket extends TCPObjectSocket implements ObjectSocket {

        public ClientObjectSocket(Socket socket, FSTConfiguration conf) throws IOException {
            super(socket, conf);
        }

        @Override
        public void setLastError(Throwable ex) {

        }

        @Override
        public Throwable getLastError() {
            return null;
        }

        @Override
        public void setConf(FSTConfiguration conf) {

        }
    }
}
