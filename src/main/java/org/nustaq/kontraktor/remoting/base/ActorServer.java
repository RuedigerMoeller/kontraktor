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
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.serialization.FSTConfiguration;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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

    protected Coding coding;
    protected FSTConfiguration conf; // parent conf
    protected RemoteRegistry reg;

    public ActorServerConnector getConnector() {
        return connector;
    }

    public ActorServer(ActorServerConnector connector, Actor facade, Coding coding) throws Exception {
        this.facade = facade;
        this.connector = connector;
        if ( coding == null )
            coding = new Coding(SerializerType.FSTSer);
        this.coding = coding;
        conf = coding.createConf();
        conf.setName("MAINCONFIG");
        RemoteRegistry.registerDefaultClassMappings(conf);
        if ( coding.getCrossPlatformShortClazzNames() != null ) {
            conf.registerCrossPlatformClassMappingUseSimpleName(coding.getCrossPlatformShortClazzNames());
        }
    }

    public void start() throws Exception {
        start(null);
    }

    public void start(Consumer<Actor> disconnectHandler) throws Exception {
        connector.connect(facade, writesocket -> {
            AtomicReference<ObjectSocket> socketRef = new AtomicReference<>(writesocket);
            reg = new RemoteRegistry( conf.deriveConfiguration(), coding) {
//            RemoteRegistry reg = new RemoteRegistry(coding) {
                @Override
                public Actor getFacadeProxy() {
                    return facade;
                }

                @Override
                public AtomicReference<ObjectSocket> getWriteObjectSocket() {
                    return socketRef;
                }
            };
            reg.setDisconnectHandler(disconnectHandler);
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
