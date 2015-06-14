package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by ruedi on 09/05/15.
 */
public class ActorServer {

    protected ActorServerConnector connector;
    protected Actor facade;
    AtomicInteger pollerCount = new AtomicInteger(0);
    protected ThreadLocal<RemoteRefPolling> poller = new ThreadLocal<RemoteRefPolling>() {
        @Override
        protected RemoteRefPolling initialValue() {
            if ( pollerCount.get() > 0 ) {
                System.out.println("more than one poller started. used poller from wrong thread ?");
                Thread.dumpStack();
            }
            pollerCount.incrementAndGet();
            return new RemoteRefPolling();
        }
    };

    // fixme: should pushed outside of this class. Currently only fst en/decoding can be used.
    // fixme: RemoteRegistry currently is responsible for configuration. bad.
    protected Coding coding;

    public ActorServerConnector getConnector() {
        return connector;
    }

    public ActorServer(ActorServerConnector connector, Actor facade, Coding coding) throws Exception {
        this.facade = facade;
        this.connector = connector;
        if ( coding == null )
            coding = new Coding(SerializerType.FSTSer);
        this.coding = coding;
    }

    public void start() throws Exception {
        connector.connect(facade, writesocket -> {
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
            Actor.current(); // ensure running in actor thread
            poller.get().scheduleSendLoop(reg);
            reg.setFacadeActor(facade);
            reg.publishActor(facade);
            return new ObjectSink() {

                @Override
                public void receiveObject(ObjectSink sink, Object received, List<IPromise> createdFutures) {
                    try {
                        reg.receiveObject(socketRef.get(), sink, received, createdFutures);
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
        return connector.closeServer();
    }

    public Actor getFacade() {
        return facade;
    }

}
