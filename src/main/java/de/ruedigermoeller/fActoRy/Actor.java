package de.ruedigermoeller.fActoRy;

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
 * Date: 03.01.14
 * Time: 21:19
 * To change this template use File | Settings | File Templates.
 */

import de.ruedigermoeller.fActoRy.impl.ActorProxyFactory;

/**
 * Baseclass for actor implementations. Note that actors are not created using constructors.
 * Use Actors.New(..) to instantiate an actor instance. To pass initialization parameter,
 * define an init method in your implementation.
 *
 * e.g.; MyActor act = Actors.New(MyActor.class); act.myInit( x,y,z );
 *
 * The init method then will be executed in the thread of the dispatcher associated with your
 * actor avoiding problems rised by state visibility inconsistency amongst threads.
 *
 * Inside an actor, everything is executed single threaded. You don't have to worry about synchronization.
 *
 * public actor methods are not allowed to return values. They must be of type void. Pass a Future.New to a call
 * in order to receive results from other actors. This does not apply to non-public methods, as they cannot be called
 * from outside the actor.
 *
 * Note that you have to pass immutable objects as arguments, else you'll get unpredictable behaviour.
 * Future versions will provide a generic deep copy for those cases, until then fall back to serialization if you
 * really have to copy large object graphs (you also might mix in traditional synchronization for shared structure access).
 *
 * code inside an actor is not allowed to ever block the current thread (networking etc.). Use Future.New and executors
 * talking to the future result handler in order to handle calls to blocking code.
 *
 */
public class Actor {

    Dispatcher dispatcher;

    /**
     * required by bytecode magic. Use Actors.New(..) to construct actor instances
     */
    public Actor() {
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public <T extends Actor> T createRef() {
        if ( this instanceof ActorProxy )
            return (T) this;
        return (T) getFactory().instantiateProxy(this);
    }

    public ActorProxyFactory getFactory() {
        return Actors.factory;
    }

}
