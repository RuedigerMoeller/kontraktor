package de.ruedigermoeller.abstraktor;

import de.ruedigermoeller.abstraktor.impl.Marshaller;

import java.lang.reflect.Method;

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
 * Time: 17:26
 * To change this template use File | Settings | File Templates.
 */

/**
 * see documentation of DefaultDispatcher
 */
public interface Dispatcher {

    Marshaller instantiateMarshaller(Actor target);
    void dispatch(ActorProxy senderRef, boolean sameThread, Actor actor, Method method, Object args[]);
    Thread getWorker();

    /**
     * stop processing messages, but do not block adding of new messages to Q
     */
    void pauseOperation();

    /**
     * continue processing messages
     */
    void continueOperation();

    /**
     * @return true if Dispatcher is not shut down
     */
    boolean isAlive();

    /**
     * @return true if Dispatcher is paused but not shut-down
     */
    boolean isPaused();

    /**
     * terminate operation after emptying Q
     */
    void shutDown();

    /**
     * terminate operation immediately. Pending messages in Q are lost
     */
    void shutDownImmediate();

    /**
     * @return decides wether this gets automatically killed
     */
    public boolean isSystemDispatcher();

}
