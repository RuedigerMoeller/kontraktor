package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.asyncio.AsyncServerSocket;
import org.nustaq.kontraktor.asyncio.ObjectAsyncSocketConnection;
import org.nustaq.kontraktor.remoting.*;
import org.nustaq.kontraktor.remoting.base.RemoteRefPolling;
import org.nustaq.serialization.FSTConfiguration;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by moelrue on 5/7/15.
 */
public class TCPActorPublisher
{

    public static Promise publish( Actor facade, int port ) {
        Promise finished = new Promise();
        facade.execute( () -> {
            TCPActorPublisher pub = new TCPActorPublisher(facade,port);
            try {
                pub.connectServerSocket();
                finished.complete();
            } catch (Exception e) {
                finished.reject(e);
            }
        });
        return finished;
    }

    int port;
    AsyncServerSocket socket;
    Coding coding = new Coding(SerializerType.FSTSer);
    Actor facade;
    FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
    RemoteRefPolling poller = new RemoteRefPolling();

    public TCPActorPublisher(Actor facade, int port) {
        this.facade = facade;
        this.port = port;
    }

    public void connectServerSocket() throws Exception {
        socket = new AsyncServerSocket();
        socket.connect(port, (key, chan) -> {

            AtomicReference<WriteObjectSocket> wsocket = new AtomicReference<>();
            RemoteRegistry reg = new RemoteRegistry(coding) {
                @Override
                public Actor getFacadeProxy() {
                    return facade;
                }
                @Override
                public AtomicReference<WriteObjectSocket> getWriteObjectSocket() {
                    return wsocket;
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

            wsocket.set(sc);
            reg.publishActor(facade);

            return sc;
        });
    }

    protected void close() throws IOException {
        socket.close();
    }

}
