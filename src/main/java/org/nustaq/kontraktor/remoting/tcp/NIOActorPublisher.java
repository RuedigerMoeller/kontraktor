package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.asyncio.AsyncServerSocket;
import org.nustaq.kontraktor.asyncio.ObjectAsyncSocketConnection;
import org.nustaq.kontraktor.remoting.*;
import org.nustaq.kontraktor.remoting.base.RemoteRefPolling;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by moelrue on 5/7/15.
 *
 * Very lightweight remote export of an actor using NIO. It does not even create a thread but can schedule
 * many connections/clients on the same thread of the exported actor is run on.
 *
 */
public class NIOActorPublisher
{

    public static Promise Publish(Actor facade, int port) {
        return Publish(facade, port, null);
    }

    public static Promise Publish(Actor facade, int port, Coding coding) {
        Promise finished = new Promise();
        NIOActorPublisher pub = new NIOActorPublisher(facade, port, coding);
        Runnable runnable = () -> {
            try {
                pub.connectServerSocket();
                finished.complete();
            } catch (Exception e) {
                finished.reject(e);
            }
        };
        facade.execute(runnable);
        return finished;
    }

    int port;
    AsyncServerSocket socket;
    Coding coding;
    Actor facade;
    RemoteRefPolling poller = new RemoteRefPolling();

    public NIOActorPublisher(Actor facade, int port, Coding coding) {
        this.facade = facade;
        this.port = port;
        this.coding = coding;
        if ( coding == null ) {
             this.coding = new Coding(SerializerType.FSTSer);
        }
    }

    public void connectServerSocket() throws Exception {
        socket = new AsyncServerSocket();
        socket.connect(port, (key, chan) -> {

            AtomicReference<WriteObjectSocket> socketRef = new AtomicReference<>();
            RemoteRegistry reg = new RemoteRegistry(coding) {
                @Override
                public Actor getFacadeProxy() {
                    return facade;
                }
                @Override
                public AtomicReference<WriteObjectSocket> getWriteObjectSocket() {
                    return socketRef;
                }
            };

            poller.scheduleSendLoop(reg);

            ObjectAsyncSocketConnection sc = new ObjectAsyncSocketConnection(reg.getConf(),key,chan) {
                @Override
                public void receivedObject(Object o) {
                    try {
                        reg.receiveObject(this,o);
                    } catch (Exception e) {
                        e.printStackTrace();
                        try {
                            close();
                        } catch (IOException e1) {
                            Actors.throwException(e1);
                        }
                    }
                }
            };

            socketRef.set(sc);
            reg.publishActor(facade);

            return sc;
        });
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    protected void close() throws IOException {
        socket.close();
    }

}
