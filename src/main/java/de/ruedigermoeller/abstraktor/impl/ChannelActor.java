package de.ruedigermoeller.abstraktor.impl;

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
 * Time: 02:59
 * To change this template use File | Settings | File Templates.
 */

import de.ruedigermoeller.abstraktor.Actor;
import de.ruedigermoeller.abstraktor.ChannelReceiver;
import de.ruedigermoeller.abstraktor.impl.Dispatcher;
import static de.ruedigermoeller.abstraktor.Actors.*;

/**
 * wrapper anonymous actors, weaving does not work on anonymous classes
 */
public class ChannelActor<T> extends Actor {

    volatile ChannelReceiver rec;
    boolean autoShut;
    boolean isFifo = false;

    Thread initThread;
    public void init(ChannelReceiver<T> rec, boolean autoShutdown ) {
        initThread = Thread.currentThread();
        this.rec = rec;
        autoShut = autoShutdown;
    }

    public void receiveResult(T result) {
        if ( initThread != null && initThread != Thread.currentThread() ) {
            throw new RuntimeException("oops "+initThread.getName()+" != "+Thread.currentThread().getName());
        }
        rec.receiveResult(result);
    }

    public void receiveError( Object error ) {
        rec.receiveError(error);
    }

    public void done() {
        if ( autoShut ) {
            getDispatcher().shutDown();
        }
    }

    @Override
    public boolean __isFIFO() {
        return isFifo;
    }

    public void finalize() {
        done();
    }

}
