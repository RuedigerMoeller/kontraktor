/*
Kontraktor-reactivestreams Copyright (c) Ruediger Moeller, All rights reserved.

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
package org.nustaq.kontraktor.reactivestreams.impl;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.reactivestreams.CancelException;
import org.nustaq.kontraktor.reactivestreams.KxReactiveStreams;
import org.nustaq.serialization.util.FSTUtil;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.Serializable;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * consuming endpoint. requests data from publisher immediately after
 * receiving onSubscribe callback
 * @param <T>
 */
public class KxSubscriber<T> implements Subscriber<T>, Serializable, Iterator<T> {
    public static final String COMPLETE = "COMPLETE";
    public static BackOffStrategy strat = new BackOffStrategy(100,2,5); // backoff when acting as iterator/stream


    protected long batchSize;
    protected Callback<T> cb;
    protected long credits;
    protected Subscription subs;
    protected boolean autoRequestOnSubs;
    protected ConcurrentLinkedQueue buffer;

    /**
     * iterator mode constructor, spawns a thread
     *
     * @param batchSize
     */
    public KxSubscriber(long batchSize) {
        this.batchSize = batchSize;
        this.autoRequestOnSubs = true;
        credits = 0;
        this.cb = (res,err) -> {
            if ( buffer == null ) {
                buffer = new ConcurrentLinkedQueue();
            }
            if ( Actors.isResult(err) ) {
                buffer.add(res);
            } else if ( Actors.isError(err) ) {
                buffer.add(err);
            } else if ( Actors.isComplete(err) ) {
                buffer.add(COMPLETE);
            }
        };
    }

    public KxSubscriber(long batchSize, Callback<T> cb) {
        this(batchSize,cb,true);
    }

    public KxSubscriber(long batchSize, Callback<T> cb, boolean autoRequestOnSubs) {
        this.batchSize = batchSize;
        this.cb = cb;
        this.autoRequestOnSubs = autoRequestOnSubs;
        credits = 0;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (subs != null) {
            s.cancel();
            return;
        }

        subs = s;
        if ( autoRequestOnSubs )
            s.request(batchSize);
        credits += batchSize;
        if ( KxPublisherActor.CRED_DEBUG )
            System.out.println("credits:" + credits );
    }

    @Override
    public void onNext(T t) {
        if ( t == null )
            throw null;
        credits--;
        if ( credits < batchSize/ KxReactiveStreams.REQU_NEXT_DIVISOR ) {
            subs.request(batchSize);
            credits += batchSize;
            if ( KxPublisherActor.CRED_DEBUG)
                System.out.println("credits:" + credits );
        }
        nextAction(t);
    }

    protected void nextAction(T t) {
        try {
            cb.stream(t);
        } catch (CancelException c) {
            subs.cancel();
        }
    }

    @Override
    public void onError(Throwable t) {
        if ( t == null )
            throw null;
        cb.reject(t);
    }

    @Override
    public void onComplete() {
        cb.finish();
    }

    /////////////////////////////////////// iterator

    Object next;
    public static ThreadLocal<Subscription> subsToCancel = new ThreadLocal<>();
    @Override
    public boolean hasNext() {
        subsToCancel.set(subs);
        int count = 0;
        while ( (buffer == null || buffer.peek() == null) ) {
            if (Actor.inside()) {
                count++;
                if ( count < 1 )
                    Actor.yield();
                else {
                    if ( count < 5 )
                        Actor.yield(1);
                    else
                        Actor.yield(5);
                }
            } else {
                strat.yield(count++);
            }
        }
        Object poll = buffer.poll();
        next = poll;
        return next != COMPLETE && next instanceof Throwable == false;
    }

    @Override
    public T next() {
        if ( next == COMPLETE ) {
            throw new RuntimeException("no further elements in iterator");
        }
        if ( next instanceof Throwable ) {
            subs.cancel();
            FSTUtil.<RuntimeException>rethrow((Throwable) next);
        }
        return (T)next;
    }
}
