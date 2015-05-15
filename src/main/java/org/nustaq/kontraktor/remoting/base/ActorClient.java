package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.impl.RemoteScheduler;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by ruedi on 10/05/15.
 */
public class ActorClient<T extends Actor> {

    protected ActorClientConnector client;
    protected Class<T> facadeClass;
    protected Coding coding;

    protected ThreadLocal<RemoteRefPolling> poller = new ThreadLocal<RemoteRefPolling>() {
        @Override
        protected RemoteRefPolling initialValue() {
            return new RemoteRefPolling();
        }
    };

    public ActorClient(ActorClientConnector client, Class<T> facadeClass, Coding coding) {
        this.facadeClass = facadeClass;
        this.client = client;
        this.coding = coding;
        if ( this.coding == null ) {
            this.coding = new Coding(SerializerType.FSTSer);
        }
    }

    public IPromise<T> connect()
    {
        Promise<T> result = new Promise<>();
        try {
            client.connect( writesocket -> {
                Actor facadeProxy = Actors.AsActor(facadeClass, new RemoteScheduler());
                facadeProxy.__remoteId = 1;

                AtomicReference<ObjectSocket> socketRef = new AtomicReference<>(writesocket);
                RemoteRegistry reg = new RemoteRegistry(coding) {
                    @Override
                    public Actor getFacadeProxy() {
                        return facadeProxy;
                    }
                    @Override
                    public AtomicReference<ObjectSocket> getWriteObjectSocket() {
                        return socketRef;
                    }
                };
                writesocket.setConf(reg.getConf());

                Actor.current(); // ensure running in actor thread
                reg.registerRemoteRefDirect(facadeProxy);
                poller.get().scheduleSendLoop(reg);
                result.resolve((T) facadeProxy);

                return new ObjectSink() {
                    @Override
                    public void receiveObject(Object received) {
                        try {
                            reg.receiveObject(socketRef.get(),this,received);
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
        } catch (Exception e) {
            if ( ! result.isSettled() )
                result.reject(e);
            else
                e.printStackTrace();
        }
        return result;
    }

}
