package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.asyncio._AsyncClientSocket;
import org.nustaq.kontraktor.asyncio.ObjectAsyncSocketConnection;
import org.nustaq.kontraktor.impl.RemoteScheduler;
import org.nustaq.kontraktor.remoting.Coding;
import org.nustaq.kontraktor.remoting.RemoteRegistry;
import org.nustaq.kontraktor.remoting.SerializerType;
import org.nustaq.kontraktor.remoting.WriteObjectSocket;
import org.nustaq.kontraktor.remoting.base.RemoteRefPolling;
import org.nustaq.kontraktor.util.Log;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by ruedi on 08/05/15.
 *
 * ALPHA has serious issues
 *
 */
public class _NIOActorClient {

    public static <T extends Actor> IPromise<T> Connect(Class<T> actorClz, Coding coding, String host, int port ) {
        _NIOActorClient client = null;
        try {
            client = new _NIOActorClient(actorClz,coding);
        } catch (IOException e) {
            return new Promise<>(null,e);
        }
        return client.connect(host,port);
    }

    public static <T extends Actor> IPromise<T> Connect(Class<T> actorClz, String host, int port ) {
        return Connect(actorClz, null, host, port);
    }

    _AsyncClientSocket socket;
    Coding coding;
    Actor facadeProxy;
    Class<? extends Actor> actorClazz;
    RemoteRegistry reg;
    RemoteRefPolling poller = new RemoteRefPolling();

    public _NIOActorClient(Class clz, Coding coding) throws IOException {
        actorClazz = clz;
        facadeProxy = Actors.AsActor(actorClazz, new RemoteScheduler());
        facadeProxy.__remoteId = 1;
        this.coding = coding;
        if ( this.coding == null ) {
            this.coding = new Coding(SerializerType.FSTSer);
        }
    }

    public  <T extends Actor> IPromise<T> connect(String host, int port) {
        Promise result = new Promise();

        socket = new _AsyncClientSocket();
        socket.connect(host, port, (key,channel) -> {

            AtomicReference<WriteObjectSocket> ref = new AtomicReference<>();

            reg = new RemoteRegistry(coding) {
                @Override
                public Actor getFacadeProxy() {
                    return facadeProxy;
                }
                @Override
                public AtomicReference<WriteObjectSocket> getWriteObjectSocket() {
                    return ref;
                }
            };

            ObjectAsyncSocketConnection osock = new ObjectAsyncSocketConnection(reg.getConf(), key, channel) {
                @Override
                public void receivedObject(Object o) {
                    try {
                        reg.receiveObject(this,o);
                    } catch (Exception e) {
                        Log.Warn(this,e);
                    }
                }
            };

            ref.set(osock);
            reg.registerRemoteRefDirect(facadeProxy);
            poller.scheduleSendLoop(reg);

            return osock;
        }).then((r, e) -> {
            if (e != null) {
                result.reject(e);
            } else {
                result.resolve(facadeProxy);
            }
        });
        return result;
    }

}
