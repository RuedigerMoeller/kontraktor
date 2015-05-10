package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by ruedi on 09/05/15.
 */
public class ActorPublisher {

    protected ActorServer server;
    protected Actor facade;
    protected RemoteRefPolling poller = new RemoteRefPolling();
    // fixme: should pushed outside of this class. Currently only fst en/decoding can be used.
    // fixme: RemoteRegistry currenlty is responsible for configuration
    protected Coding coding;

    public ActorPublisher(ActorServer server, Actor facade, Coding coding) throws Exception {
        this.facade = facade;
        this.server = server;
        if ( coding == null )
            coding = new Coding(SerializerType.FSTSer);
        this.coding = coding;
    }

    public void start() throws Exception {
        server.connect(facade, writesocket -> {
            AtomicReference<ObjectSocket> socketRef = new AtomicReference<>(writesocket);
            RemoteRegistry reg = new RemoteRegistry(coding) {
                @Override
                public Actor getFacadeProxy() {
                    return facade;
                }
                @Override
                public AtomicReference<ObjectSocket> getWriteObjectSocket() {
                    return socketRef;
                }
            };
            writesocket.setConf(reg.getConf());
            poller.scheduleSendLoop(reg);
            reg.publishActor(facade);
            return new ObjectSink() {
                @Override
                public void receiveObject(Object received) {
                    try {
                        reg.receiveObject(socketRef.get(),received);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void sinkClosed() {
                    reg.setTerminated(true);
                    reg.cleanUp();
                }
            };
        });
    }

    public IPromise close() {
        return server.closeServer();
    }

    public Actor getFacade() {
        return facade;
    }

}
