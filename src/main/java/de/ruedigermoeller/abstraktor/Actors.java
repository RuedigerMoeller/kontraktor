package de.ruedigermoeller.abstraktor;

import de.ruedigermoeller.abstraktor.impl.ActorProxyFactory;
import de.ruedigermoeller.abstraktor.impl.Dispatcher;

import java.util.concurrent.ConcurrentLinkedDeque;

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

    static Actors instance = new Actors();

    /**
     * allowed to be set only by dispatcher instances from their associated thread.
     * Do not modify externally !
     */
    public static ThreadLocal<Dispatcher> threadDispatcher = new ThreadLocal<Dispatcher>();

    public static <T extends Actor> T New( Class<? extends Actor> actorClazz ) {
        return (T) instance.newProxy(actorClazz);
    }

    public static <T extends Actor> T New( Class<? extends Actor> actorClazz, Dispatcher disp ) {
        return (T) instance.newProxy(actorClazz,disp);
    }

    /**
     * @return a new dispatcher backed by a new thread. Use to isolate blocking code only, Else use AnyDispatcher instead
     */
    public static Dispatcher NewDispatcher() {
        try {
            return instance.newDispatcher();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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
        if ( threadDispatcher.get() != null ) {
            return newProxy( clz, threadDispatcher.get() );
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
