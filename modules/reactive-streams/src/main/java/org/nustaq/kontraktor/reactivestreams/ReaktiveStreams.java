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
package org.nustaq.kontraktor.reactivestreams;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.base.ActorPublisher;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.util.Log;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Created by ruedi on 28/06/15.
 *
 * Factory class for reactive streams <=> Kontraktor interop.
 *
 * Note you rarely need use this directly. To publish / start a stream, see EventSink (offer() like feed in point),
 * this also allows to publish it on network.
 *
 * For connecting a remote stream, see ReaktiveStreams.connectXXX methods. Note that KPublisher extends the original
 * Publisher interface.
 *
 * for interop use asKPublisher()
 *
 */
public class ReaktiveStreams extends Actors {

    // as for remoting the request(N) ack signals have network latency
    // request(N) is executed with large sizes to not let the sending side "dry"/stall.
    // event sizes below reduce throughput by 10..30% percent on a localhost.
    // in case of high latency connections (wifi, WAN) sizes below probably need to be raised
    public static final int MAX_BATCH_SIZE = 50_000;
    public static int DEFAULT_BATCH_SIZE = 50_000;
    public static int DEFAULTQSIZE = 128_000; // needs to be > 2*MAX_BATCH_SIZE !!

    public static int REQU_NEXT_DIVISOR = 1;

    protected static ReaktiveStreams instance = new ReaktiveStreams();

    public static ReaktiveStreams get() {
        return instance;
    }

    ////////// singleton instance methods ////////////////////////////////////////////////

    /**
     * interop, obtain a KPublisher from an arbitrary rxstreams publisher.
     *
     * KPublisher inherits rxtreams.Publisher and adds some sugar like map, network publish etc. via default methods
     *
     * @param p
     * @param <T>
     * @return
     */
    public <T> RxPublisher<T> asRxPublisher(Publisher<T> p) {
        if ( p instanceof RxPublisher)
            return (RxPublisher<T>) p;
        return new RxPublisher<T>() {
            @Override
            public void subscribe(Subscriber<? super T> s) {
                p.subscribe(s);
            }
        };
    }

    /**
     * consuming endpoint. requests data from publisher immediately after
     * receiving onSubscribe callback. Maps from reactive streams Subscriber to Kontraktor Callback
     *
     * all events are routed to the callback
     * e.g.
     * <pre>
            subscriber( (event, err) -> {
                if (Actors.isComplete(err)) {
                    System.out.println("complete");
                } else if (Actors.isError(err)) {
                    System.out.println("ERROR");
                } else {
                    // process event
                }
            }
       </pre>
     *
     * @param <T>
     */
    public <T> Subscriber<T> subscriber(Callback<T> cb) {
        return subscriber(DEFAULT_BATCH_SIZE,cb);
    }

    /**
     * consuming endpoint. requests data from publisher immediately after
     * receiving onSubscribe callback. Maps from reactive streams Subscriber to Kontraktor Callback
     *
     * all events are routed to the callback
     * e.g.
     * <pre>
            subscriber( (event, err) -> {
                if (Actors.isComplete(err)) {
                    System.out.println("complete");
                } else if (Actors.isError(err)) {
                    System.out.println("ERROR");
                } else {
                    // process event
                }
            }
       </pre>
     *
     * @param <T>
     */
    public <T> Subscriber<T> subscriber(int batchSize, Callback<T> cb) {
        if ( batchSize > MAX_BATCH_SIZE ) {
            throw new RuntimeException("batch size exceeds maximum of "+MAX_BATCH_SIZE);
        }
        return new MySubscriber<>(batchSize, cb);
    }

    public <T> RxPublisher<T> connect( Class<T> eventType, ConnectableActor connectable ) {
        return (RxPublisher<T>) connect(eventType, connectable, null).await();
    }

    public <T> RxPublisher<T> produce( Stream<T> stream ) {
        return produce(stream.iterator(),DEFAULT_BATCH_SIZE);
    }

    public <T> RxPublisher<T> produce( Collection<T> collection ) {
        return produce(collection.iterator(),DEFAULT_BATCH_SIZE);
    }

    public RxPublisher<Integer> produce( IntStream stream ) {
        return produce(stream.mapToObj( i->i ).iterator(),DEFAULT_BATCH_SIZE);
    }

    public RxPublisher<Long> produce( LongStream stream ) {
        return produce(stream.mapToObj( i->i ).iterator(),DEFAULT_BATCH_SIZE);
    }

    public RxPublisher<Double> produce( DoubleStream stream ) {
        return produce(stream.mapToObj( i->i ).iterator(),DEFAULT_BATCH_SIZE);
    }

    public <T> RxPublisher<T> produce( Stream<T> stream, int batchSize ) {
        return produce(stream.iterator(),batchSize);
    }

    public <T> RxPublisher<T> produce( Iterator<T> iter ) {
        return produce(iter,DEFAULT_BATCH_SIZE);
    }

    public <T> RxPublisher<T> produce( Iterator<T> iter, int batchSize ) {
        RxPublisherActor pub = Actors.AsActor(RxPublisherActor.class, DEFAULTQSIZE);
        if ( batchSize > ReaktiveStreams.MAX_BATCH_SIZE ) {
            throw new RuntimeException("batch size exceeds max of "+ReaktiveStreams.MAX_BATCH_SIZE);
        }
        pub.setBatchSize(batchSize);
        pub.setThrowExWhenBlocked(true);
        pub.initFromIterator(iter);
        return pub;
    }

    /**
     * @param eventType
     * @param connectable
     * @param disconHandler - can be null
     * @param <T>
     * @return
     */
    public <T> IPromise<RxPublisher<T>> connect(Class<T> eventType, ConnectableActor connectable, Callback<ActorClientConnector> disconHandler) {
        Callback<ActorClientConnector> discon = (acc,err) -> {
            Log.Info(this, "Client disconnected");
            acc.closeClient();
            if ( disconHandler != null ) {
                disconHandler.complete(acc,err);
            }
        };
        return connectable.actorClass(RxPublisherActor.class).inboundQueueSize(DEFAULTQSIZE).connect(discon, remoteref -> {
            if ( ((RxPublisherActor)remoteref).callerSideSubscribers != null ) {
                ((RxPublisherActor)remoteref).callerSideSubscribers.forEach( subs -> {
                    ((Subscriber)subs).onError(new IOException("connection lost"));
                });
                ((RxPublisherActor)remoteref).callerSideSubscribers = null;
            }
        });
    }

    /**
     * exposes a publisher on the network via kontraktor's generic remoting. Usually not called directly (see EventSink+KPublisher)
     *
     * @param source
     * @param networkPublisher - the appropriate network publisher (TCP,TCPNIO,WS)
     * @param disconCB         - called once a client disconnects/stops. can be null
     * @param <OUT>
     * @return
     */
    public <OUT> IPromise serve(Publisher<OUT> source, ActorPublisher networkPublisher, boolean closeConnectionOnCompleteOrError, Consumer<Actor> disconCB) {
        if ( networkPublisher.getClass().getSimpleName().equals("HttpPublisher") ) {
            throw new RuntimeException("Http long poll cannot be supported. Use WebSockets instead.");
        }
        if (source instanceof RxPublisherActor == false || source instanceof ActorProxy == false ) {
            Processor<OUT, OUT> proc = newAsyncProcessor(a -> a); // we need a queue before going to network
            source.subscribe(proc);
            source = proc;
        }
        ((RxPublisherActor)source).setCloseOnComplete(closeConnectionOnCompleteOrError);
        return networkPublisher.facade((Actor) source).publish(disconCB);
    }

    /**
     * create async processor. Usually not called directly (see EventSink+KPublisher)
     *
     * @param processingFunction
     * @param <IN>
     * @param <OUT>
     * @return
     */
    public <IN, OUT> Processor<IN, OUT> newAsyncProcessor(Function<IN, OUT> processingFunction) {
        return newAsyncProcessor(processingFunction, new SimpleScheduler(DEFAULTQSIZE), DEFAULT_BATCH_SIZE);
    }

    /**
     *
     * create async processor. Usually not called directly (see EventSink+KPublisher)
     *
     * @param processingFunction
     * @param batchSize
     * @param <IN>
     * @param <OUT>
     * @return
     */
    public <IN, OUT> Processor<IN, OUT> newAsyncProcessor(Function<IN, OUT> processingFunction, int batchSize) {
        return newAsyncProcessor(processingFunction, new SimpleScheduler(DEFAULTQSIZE), batchSize);
    }

    /**
     * create async processor. Usually not called directly (see EventSink+KPublisher)
     *
     * @param processingFunction
     * @param sched
     * @param batchSize
     * @param <IN>
     * @param <OUT>
     * @return
     */
    public <IN, OUT> Processor<IN, OUT> newAsyncProcessor(Function<IN, OUT> processingFunction, Scheduler sched, int batchSize) {
        RxPublisherActor pub = Actors.AsActor(RxPublisherActor.class, sched);
        if ( batchSize > ReaktiveStreams.MAX_BATCH_SIZE ) {
            throw new RuntimeException("batch size exceeds max of "+ReaktiveStreams.MAX_BATCH_SIZE);
        }
        pub.setBatchSize(batchSize);
        pub.setThrowExWhenBlocked(true);
        pub.init(processingFunction);
        return pub;
    }

    /**
     *
     * create sync processor. Usually not called directly (see EventSink+KPublisher)
     *
     * @param processingFunction
     * @param <IN>
     * @param <OUT>
     * @return
     */
    public <IN,OUT> Processor<IN,OUT> newSyncProcessor(Function<IN, OUT> processingFunction) {
        SyncProcessor<IN, OUT> inoutSyncProcessor = new SyncProcessor<>(DEFAULT_BATCH_SIZE, processingFunction);
        return inoutSyncProcessor;
    }

    /**
     * consuming endpoint. requests data from publisher immediately after
     * receiving onSubscribe callback
     * @param <T>
     */
    protected static class MySubscriber<T> implements Subscriber<T>, Serializable {
        protected long batchSize;
        protected Callback<T> cb;
        protected long credits;
        protected Subscription subs;
        protected boolean autoRequestOnSubs;

        public MySubscriber(long batchSize, Callback<T> cb ) {
            this(batchSize,cb,true);
        }

        public MySubscriber(long batchSize, Callback<T> cb, boolean autoRequestOnSubs ) {
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
            if ( RxPublisherActor.CRED_DEBUG )
                System.out.println("credits:" + credits );
        }

        @Override
        public void onNext(T t) {
            if ( t == null )
                throw null;
            credits--;
            if ( credits < batchSize/ReaktiveStreams.REQU_NEXT_DIVISOR ) {
                subs.request(batchSize);
                credits += batchSize;
                if ( RxPublisherActor.CRED_DEBUG)
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
    }

    protected static class SyncProcessor<IN, OUT> implements Processor<IN, OUT>, RxPublisher<OUT> {

        protected Subscription inSubs;
        protected Subscription outSubs;
        protected Subscriber<OUT> subscriber;
        protected boolean done = false;
        protected long batchSize;
        protected Function<IN,OUT> proc;
        protected long initialRequest = 0;

        public SyncProcessor(long batchSize, Function<IN, OUT> proc) {
            this.batchSize = batchSize;
            this.proc = proc;
        }

        @Override
        public void onSubscribe(final Subscription s) {
            if (s == null)
                throw null;
            if (inSubs != null) {
                s.cancel();
                return;
            }
            inSubs = s;
            if ( initialRequest > 0 )
                s.request(initialRequest);
        }

        @Override
        public void onNext(final IN element) {
            if ( done )
                return;
            if (element == null)
                throw null;
            try {
                subscriber.onNext(process(element));
            } catch (final Throwable t) {
                onError(t);
            }
        }

        protected OUT process(final IN element) {
            return proc.apply(element);
        }

        @Override
        public void onError(final Throwable t) {
            if ( subscriber != null )
                subscriber.onError(t);
            else
                t.printStackTrace();
        }

        @Override
        public void onComplete() {
            done = true;
            subscriber.onComplete();
        }

        @Override
        public void subscribe(Subscriber<? super OUT> s) {
            if ( subscriber != null ) {
                throw new RuntimeException("can only subscribe once");
            }
            subscriber = (Subscriber<OUT>) s;
            s.onSubscribe( outSubs = new MySubs());
        }

        protected class MySubs implements Subscription {
            @Override
            public void request(long n) {
                if ( n <= 0 ) {
                    subscriber.onError(new IllegalArgumentException("rule 3.9: request > 0 elements"));
                    return;
                }
                if ( inSubs != null )
                    inSubs.request(n);
                else
                    initialRequest = n;
            }

            @Override
            public void cancel() {
                inSubs.cancel();
                subscriber = null;
            }

        }
    }
}
