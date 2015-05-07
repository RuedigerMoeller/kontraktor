package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.asyncio.AsyncServerSocket;
import org.nustaq.kontraktor.asyncio.ObjectAsyncSocketConnection;
import org.nustaq.kontraktor.remoting.*;
import org.nustaq.serialization.FSTConfiguration;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by moelrue on 5/7/15.
 */
public class TCPActorPublisher
{
    int port;
    AsyncServerSocket socket;
    Coding coding = new Coding(SerializerType.FSTSer);
    Actor facade;
    FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

    public TCPActorPublisher(Actor proxy, int port) {
        this.facade = facade;
        this.port = port;
    }

    public void connectServerSocket() throws Exception {
        socket = new AsyncServerSocket();
        socket.connect(port, (key, chan) -> {

            RemoteRefRegistry reg = new RemoteRefRegistry(coding) {
                @Override
                public Actor getFacadeProxy() {
                    return facade;
                }

                @Override
                public AtomicReference<ObjectSocket> getObjectSocket() {
                    return null; // FIMXE: get rid of that midterm. Interface implies sync communication
                }
            };

            ObjectAsyncSocketConnection sc = new ObjectAsyncSocketConnection(reg.getConf(),key,chan) {
                @Override
                public void receivedObject(Object o) {
                    try {
                        reg.receiveObject(null,o);
                    } catch (Exception e) {
                        e.printStackTrace();
                        closed(e);
                    }
                }
            };
            return sc;
        });
    }

    protected void close() throws IOException {
        socket.close();
    }
}
