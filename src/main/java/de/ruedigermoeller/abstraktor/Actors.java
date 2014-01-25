package de.ruedigermoeller.abstraktor;

import de.ruedigermoeller.abstraktor.impl.ActorProxyFactory;
import de.ruedigermoeller.abstraktor.impl.ChannelActor;
import de.ruedigermoeller.abstraktor.impl.Dispatcher;

/**
 * Copyright (c) 2012, Ruediger Moeller. All rights reserved.
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 * <p/>
 * Date: 04.01.14
 * Time: 19:50
 * To change this template use File | Settings | File Templates.
 */
public class Actors {

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // static API

    static Actors instance = new Actors();

    public static <T extends Actor> T AsActor(Class<? extends Actor> actorClazz) {
        return (T) instance.newProxy(actorClazz);
    }

    public static <T extends Actor> T AsActor(Class<? extends Actor> actorClazz, Dispatcher disp) {
        return (T) instance.newProxy(actorClazz,disp);
    }

    public static <T extends Actor> T SpawnActor(Class<? extends Actor> actorClazz) {
        return (T) instance.newProxy(actorClazz, createDispatcher() );
    }

    /**
     * Create a new future actor inheriting the dispatcher from the calling actor.
     * If a future is created from a non-actor context, a new DispatcherThread will be created,
     * which is automatically terminated after the one message has been received by the
     * future.
     * @param rec
     * @param <R>
     * @return
     */
    public static <R> ChannelActor<R> Channel(ChannelReceiver<R> rec) {
        return Queue(null, false, rec);
    }

    public static <R> ChannelActor<R> QueuedChannel(ChannelReceiver<R> rec) {
        Dispatcher disp = Actors.createDispatcher();
        ((Dispatcher)disp).setName("SpawnActor ChannelActor "+rec.getClass().getName());
        return Queue(disp, true, rec);
    }

    public static <R> ChannelActor<R> Queue(Dispatcher disp, boolean autoShut, ChannelReceiver<R> rec) {
        // autoShutdown is not applied if a shared dispatcher is used.
        if ( disp == null ) {
            disp = Dispatcher.getThreadDispatcher();
            autoShut = false;
        } else {
            //autoShut = ! disp.isSystemDispatcher() && autoShut;
        }
        ChannelActor res = null;
        if ( disp != null ) {
            res = AsActor(ChannelActor.class, disp);
        } else {
            Dispatcher newDisp = Actors.createDispatcher();
            autoShut = true;
            newDisp.setName("ChannelActor "+rec.getClass().getName());
            res = AsActor(ChannelActor.class, newDisp);
        }
        rec.fut = (ChannelActor) res.getActor();
        res.init(rec, autoShut);
        res.getActor().sync();
        return res;
    }

    /**
     * @return a new dispatcher backed by a new thread. Use to isolate blocking code only, Else use AnyDispatcher instead
     */
    public static Dispatcher createDispatcher() {
        try {
            return instance.newDispatcher();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // end static API
    //
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    protected Actors() {
        factory = new ActorProxyFactory();
    }

    protected ActorProxyFactory factory;

    protected ActorProxyFactory getFactory() {
        return factory;
    }

    protected Actor newProxy(Class<? extends Actor> clz, Dispatcher disp ) {
        try {
            Actor res = clz.newInstance();
            res.dispatcher = disp;
            Actor proxy = getFactory().instantiateProxy(res);
            return proxy;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected Actor newProxy(Class<? extends Actor> clz) {
        if ( Dispatcher.getThreadDispatcher() != null ) {
            return newProxy( clz, Dispatcher.getThreadDispatcher() );
        } else {
            try {
                return newProxy(clz, newDispatcher());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * return a new dispatcher backed by a new thread. Overriding classes should *not*
     * return existing dispatchers here, as this can be used to isolate blocking code from the actor flow.
     *
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    protected Dispatcher newDispatcher() throws InstantiationException, IllegalAccessException {
        return new Dispatcher();
    }

}
