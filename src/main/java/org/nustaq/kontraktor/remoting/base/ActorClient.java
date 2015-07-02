/*
Kontraktor Copyright (c) Ruediger Moeller, All rights reserved.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3.0 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

See https://www.gnu.org/licenses/lgpl.txt
*/

package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.impl.RemoteScheduler;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.serialization.util.FSTUtil;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by ruedi on 10/05/15.
 *
 * Base class of client role connections to remote actors (published)
 *
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

    public IPromise<T> connect() {
        return connect(RemoteScheduler.DEFQSIZE);
    }

    public IPromise<T> connect(int qsiz)
    {
        Promise<T> result = new Promise<>();
        try {
            client.connect( writesocket -> {
                Actor facadeProxy = Actors.AsActor(facadeClass, new RemoteScheduler(qsiz));
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
                if ( coding.getCrossPlatformShortClazzNames() != null )
                   reg.getConf().registerCrossPlatformClassMappingUseSimpleName(coding.getCrossPlatformShortClazzNames());
                writesocket.setConf(reg.getConf());

                Actor.current(); // ensure running in actor thread
                reg.registerRemoteRefDirect(facadeProxy);
                poller.get().scheduleSendLoop(reg);
                result.resolve((T) facadeProxy);

                return new ObjectSink() {
                    @Override
                    public void receiveObject(ObjectSink sink, Object received, List<IPromise> createdFutures) {
                        try {
                            reg.receiveObject(socketRef.get(),sink,received, createdFutures);
                        } catch (Exception e) {
                            FSTUtil.rethrow(e);
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
