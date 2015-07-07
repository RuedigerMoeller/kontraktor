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


    static Integer zero = Integer.valueOf(0);

    protected long batchSize;
    protected Callback<T> cb;
    protected long credits;
    protected Subscription subs;
    protected boolean autoRequestOnSubs;
    protected volatile Object complete = 0; // 0 - in operation, "COMPLETE" = complete, else = error
    protected ConcurrentLinkedQueue<T> buffer;

    /**
     * iterator mode constructor, spawns a thread
     *
     * @param batchSize
     */
    public KxSubscriber(long batchSize) {
        this.batchSize = batchSize;
        this.autoRequestOnSubs = true;
        credits = 0;
        complete = zero;
        this.cb = (res,err) -> {
            if ( buffer == null ) {
                buffer = new ConcurrentLinkedQueue();
            }
            if ( Actors.isResult(err) ) {
                buffer.add(res);
            } else if ( Actors.isError(err) ) {
                complete = err;
            } else if ( Actors.isComplete(err) ) {
                complete = COMPLETE;
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

    T next;
    @Override
    public boolean hasNext() {
        int count = 0;
        while ( (buffer == null || buffer.peek() == null) && complete == zero ) {
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
        next = buffer.poll();
        return complete != COMPLETE;
    }

    @Override
    public T next() {
        if ( complete != zero && complete != COMPLETE ) {
            if ( complete instanceof Throwable )
                FSTUtil.<RuntimeException>rethrow((Throwable) complete);
        }
        if ( complete == COMPLETE )
            throw new RuntimeException("unexpected iterator state");
        return next;
    }
}
