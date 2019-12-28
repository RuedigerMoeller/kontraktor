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
import org.nustaq.kontraktor.annotations.*;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.FSTConfiguration;

import java.io.IOException;
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

    public ActorServerConnector getConnector() {
        return connector;
    }

    public ActorServer(ActorServerConnector connector, Actor facade, Coding coding) throws Exception {
        this.facade = facade;
        if ( facade.getActor().getClass().getAnnotation(Local.class) != null )
            throw new RuntimeException("Local Actor cannot be remoted: "+facade.getActor().getClass().getName());
        this.connector = connector;
        if ( coding == null )
            coding = new Coding(SerializerType.FSTSer);
        this.coding = coding;
        conf = coding.createConf();
        conf.setName("MAINCONFIG");
        ConnectionRegistry.registerDefaultClassMappings(conf);
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
            ConnectionRegistry reg = new ConnectionRegistry( conf.deriveConfiguration(), coding) {
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
            //reg.constraints(connector.getConstraints());
            writesocket.setConf(reg.getConf());
            Actor.current(); // ensure running in actor thread
            poller.get().scheduleSendLoop(reg);
            reg.setFacadeActor(facade);
            reg.publishActor(facade);
            reg.setServer(this);
            Log.Info(this, "connected a client with registry "+System.identityHashCode(reg)+", "+writesocket.getConnectionIdentifier() );
            if ( facade instanceof ServingActor ) {
                ((ServingActor) facade).clientConnected(reg,writesocket.getConnectionIdentifier());
            }
            return new ObjectSink() {
                protected boolean closed = false;

                @Override
                public void receiveObject(ObjectSink sink, Object received, List<IPromise> createdFutures, Object securityContext) {
                    try {
                        reg.receiveObject(socketRef.get(), sink, received, createdFutures, securityContext);
                    } catch (Exception e) {
                        Log.Error(this,e);
                    }
                }

                @Override
                public void sinkClosed() {
                    if ( closed ) {
                        return;
                    }
                    Log.Info(ActorServer.this,"disconnected a client "+System.identityHashCode(reg)+", "+writesocket.getConnectionIdentifier());
                    if ( facade instanceof ServingActor ) {
                        ((ServingActor) facade).clientDisconnected(reg,writesocket.getConnectionIdentifier());
                    }
                    reg.disconnect();
                    try {
                        closed = true;
                        writesocket.close();
                    } catch (IOException e) {
                        Log.Info(this,e);
                    }
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
