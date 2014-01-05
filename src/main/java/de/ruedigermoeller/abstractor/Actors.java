package de.ruedigermoeller.abstractor;

import de.ruedigermoeller.abstractor.impl.DefaultDispatcher;
import de.ruedigermoeller.abstractor.impl.ActorProxyFactory;

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
    public static ActorProxyFactory factory = new ActorProxyFactory();
    /**
     * default implementation to use for dispatchers. requires no-arg constructor
     */
    public static Class<? extends Dispatcher> defaultDispatcherClass = DefaultDispatcher.class;
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

    public static Dispatcher NewDispatcher() {
        try {
            return instance.newDispatcher();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    protected Dispatcher newDispatcher() throws InstantiationException, IllegalAccessException {
        return defaultDispatcherClass.newInstance();
    }

}
