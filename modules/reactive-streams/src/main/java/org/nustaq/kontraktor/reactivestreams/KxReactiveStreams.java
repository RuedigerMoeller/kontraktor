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
import org.nustaq.kontraktor.reactivestreams.impl.KxSubscriber;
import org.nustaq.kontraktor.reactivestreams.impl.KxPublisherActor;
import org.nustaq.kontraktor.reactivestreams.impl.SyncProcessor;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.base.ActorPublisher;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.util.Log;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.*;

/**
 * Created by ruedi on 28/06/15.
 *
 * Factory class for reactive streams <=> Kontraktor interop.
 *
 * Note you rarely need use this directly. To publish / start a stream, see EventSink (offer() like feed in point),
 * this also allows to publish it on network.
 *
 * For connecting a remote stream, see ReaktiveStreams.connectXXX methods. Note that RxPublisher extends the original
 * Publisher interface.
 *
 * for interop use asRxPublisher()
 *
 */
public class KxReactiveStreams extends Actors {

    // as for remoting the request(N) ack signals have network latency
    // request(N) is executed with large sizes to not let the sending side "dry"/stall.
    // event sizes below reduce throughput by 10..30% percent on a localhost.
    // in case of high latency connections (wifi, WAN) sizes below probably need to be raised
    public static final int MAX_BATCH_SIZE = 50_000;
    public static int DEFAULT_BATCH_SIZE = 50_000;
    public static int DEFAULTQSIZE = 128_000; // needs to be > 2*MAX_BATCH_SIZE !!

    public static int REQU_NEXT_DIVISOR = 1;

    protected static KxReactiveStreams instance = new KxReactiveStreams();

    public static KxReactiveStreams get() {
        return instance;
    }

    ////////// singleton instance methods ////////////////////////////////////////////////

    /**
     * interop, obtain a RxPublisher from an arbitrary rxstreams publisher.
     *
     * RxPublisher inherits rxtreams.Publisher and adds some sugar like map, network publish etc. via default methods
     *
     * @param p
     * @param <T>
     * @return
     */
    public <T> KxPublisher<T> asRxPublisher(Publisher<T> p) {
        if ( p instanceof KxPublisher)
            return (KxPublisher<T>) p;
        return new KxPublisher<T>() {
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
        return new KxSubscriber<>(batchSize, cb);
    }

    public <T> KxPublisher<T> connect( Class<T> eventType, ConnectableActor connectable ) {
        return (KxPublisher<T>) connect(eventType, connectable, null).await();
    }

    public <T> KxPublisher<T> produce( Stream<T> stream ) {
        return produce(DEFAULT_BATCH_SIZE,stream.iterator());
    }

    public <T> KxPublisher<T> produce( Collection<T> collection ) {
        return produce(DEFAULT_BATCH_SIZE, collection.iterator());
    }

    public KxPublisher<Integer> produce( IntStream stream ) {
        return produce( DEFAULT_BATCH_SIZE, stream.mapToObj( i->i ).iterator() );
    }

    public KxPublisher<Long> produce( LongStream stream ) {
        return produce( DEFAULT_BATCH_SIZE, stream.mapToObj( i->i ).iterator());
    }

    public KxPublisher<Double> produce( DoubleStream stream ) {
        return produce(DEFAULT_BATCH_SIZE,stream.mapToObj( i->i ).iterator());
    }

    public <T> KxPublisher<T> produce( int batchSize, Stream<T> stream ) {
        return produce(batchSize,stream.iterator());
    }

    public <T> KxPublisher<T> produce( Iterator<T> iter ) {
        return produce(DEFAULT_BATCH_SIZE,iter);
    }

    public <T> KxPublisher<T> produce( int batchSize, Iterator<T> iter ) {
        KxPublisherActor pub = Actors.AsActor(KxPublisherActor.class, DEFAULTQSIZE);
        if ( batchSize > KxReactiveStreams.MAX_BATCH_SIZE ) {
            throw new RuntimeException("batch size exceeds max of "+ KxReactiveStreams.MAX_BATCH_SIZE);
        }
        pub.setBatchSize(batchSize);
        pub.setThrowExWhenBlocked(true);
        pub.initFromIterator(iter);
        return pub;
    }

    /**
     * warning: blocks the calling thread. Need to execute in a separate thread if called
     * from within a callback
     */
    public <T> Stream<T> stream( Publisher<T> pub ) {
        return stream(pub, DEFAULT_BATCH_SIZE);
    }

    /**
     * warning: blocks the calling thread. Need to execute in a separate thread if called
     * from within a callback
     */
    public <T> Stream<T> stream( Publisher pub, int batchSize ) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(pub, batchSize), Spliterator.IMMUTABLE | Spliterator.NONNULL), false);
    }

    /**
     * warning: blocks the calling thread. Need to execute in a separate thread if called
     * from within a callback
     */
    public <T> Iterator<T> iterator( Publisher<T> pub ) {
        return iterator(pub, DEFAULT_BATCH_SIZE);
    }

    /**
     * warning: blocks the calling thread. Need to execute in a separate thread if called
     * from within a callback
     */
    public <T> Iterator<T> iterator( Publisher<T> pub, int batchSize ) {
        if ( batchSize > KxReactiveStreams.MAX_BATCH_SIZE ) {
            throw new RuntimeException("batch size exceeds max of "+ KxReactiveStreams.MAX_BATCH_SIZE);
        }
        KxSubscriber<T> subs = new KxSubscriber<T>(batchSize);
        pub.subscribe(subs);

        return subs;
    }

    /**
     * @param eventType
     * @param connectable
     * @param disconHandler - can be null
     * @param <T>
     * @return
     */
    public <T> IPromise<KxPublisher<T>> connect(Class<T> eventType, ConnectableActor connectable, Callback<ActorClientConnector> disconHandler) {
        Callback<ActorClientConnector> discon = (acc,err) -> {
            Log.Info(this, "Client disconnected");
            acc.closeClient();
            if ( disconHandler != null ) {
                disconHandler.complete(acc,err);
            }
        };
        return connectable.actorClass(KxPublisherActor.class).inboundQueueSize(DEFAULTQSIZE).connect(discon, remoteref -> {
            if ( ((KxPublisherActor)remoteref)._callerSideSubscribers != null ) {
                ((KxPublisherActor)remoteref)._callerSideSubscribers.forEach( subs -> {
                    ((Subscriber)subs).onError(new IOException("connection lost"));
                });
                ((KxPublisherActor)remoteref)._callerSideSubscribers = null;
            }
        });
    }

    /**
     * exposes a publisher on the network via kontraktor's generic remoting. Usually not called directly (see EventSink+RxPublisher)
     *
     * @param source
     * @param networRxPublisher - the appropriate network publisher (TCP,TCPNIO,WS)
     * @param disconCB         - called once a client disconnects/stops. can be null
     * @param <OUT>
     * @return
     */
    public <OUT> IPromise serve(Publisher<OUT> source, ActorPublisher networRxPublisher, boolean closeConnectionOnCompleteOrError, Consumer<Actor> disconCB) {
        if ( networRxPublisher.getClass().getSimpleName().equals("HttpPublisher") ) {
            throw new RuntimeException("Http long poll cannot be supported. Use WebSockets instead.");
        }
        if (source instanceof KxPublisherActor == false || source instanceof ActorProxy == false ) {
            Processor<OUT, OUT> proc = newAsyncProcessor(a -> a); // we need a queue before going to network
            source.subscribe(proc);
            source = proc;
        }
        ((KxPublisherActor)source).setCloseOnComplete(closeConnectionOnCompleteOrError);
        return networRxPublisher.facade((Actor) source).publish(disconCB);
    }

    /**
     * create async processor. Usually not called directly (see EventSink+RxPublisher)
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
     * create async processor. Usually not called directly (see EventSink+RxPublisher)
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
     * create async processor. Usually not called directly (see EventSink+RxPublisher)
     *
     * @param processingFunction
     * @param sched
     * @param batchSize
     * @param <IN>
     * @param <OUT>
     * @return
     */
    public <IN, OUT> Processor<IN, OUT> newAsyncProcessor(Function<IN, OUT> processingFunction, Scheduler sched, int batchSize) {
        KxPublisherActor pub = Actors.AsActor(KxPublisherActor.class, sched);
        if ( batchSize > KxReactiveStreams.MAX_BATCH_SIZE ) {
            throw new RuntimeException("batch size exceeds max of "+ KxReactiveStreams.MAX_BATCH_SIZE);
        }
        pub.setBatchSize(batchSize);
        pub.setThrowExWhenBlocked(true);
        pub.init(processingFunction);
        return pub;
    }

    /**
     *
     * create sync processor. Usually not called directly (see EventSink+RxPublisher)
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

}
