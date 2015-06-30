package org.nustaq.kontraktor.reactivestreams;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.base.ActorPublisher;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by ruedi on 28/06/15.
 */
public class ReaktiveStreams extends Actors {

    protected static final int MAX_BATCH_SIZE = 5000;
    public static int DEFAULT_BATCH_SIZE = 3_000;
    public static int DEFAULTQSIZE = 128_000;
    public static int REQU_NEXT_DIVISOR = 1;

    protected static ReaktiveStreams instance = new ReaktiveStreams();

    public static ReaktiveStreams get() {
        return instance;
    }

    /////////////////// static helpers /////////////////////////////////////////

    /**
     * consuming endpoint. requests data from publisher immediately after
     * receiving onSubscribe callback
     * @param <T>
     */
    public static <T> Subscriber<T> subscriber(Callback<T> cb) {
        return subscriber(DEFAULT_BATCH_SIZE,cb);
    }

    /**
     * consuming endpoint. requests data from publisher immediately after
     * receiving onSubscribe callback
     * @param <T>
     */
    public static <T> Subscriber<T> subscriber(int batchSize, Callback<T> cb) {
        if ( batchSize > MAX_BATCH_SIZE ) {
            throw new RuntimeException("batch size exceeds maximum of "+MAX_BATCH_SIZE);
        }
        return new MySubscriber<>(batchSize, cb);
    }

    public static <T> Publisher<T> connect( Class eventType, ConnectableActor connectable ) {
        return (Publisher<T>) get().connectRemotePublisher(eventType, connectable, null ).await();
    }

    public static <T> Publisher<T> connect( Class eventType, ConnectableActor connectable, Callback<ActorClientConnector> disconCB ) {
        return (Publisher<T>) get().connectRemotePublisher(eventType, connectable, disconCB).await();
    }

    ////////// singleton instance methods ////////////////////////////////////////////////

    /**
     * exposes a publisher on the network via kontraktor's generic remoting
     *
     * @param source
     * @param networkPublisher - the appropriate network publisher (TCP,TCPNIO,WS,HTTP)
     * @param disconCB         - called once a client disconnects. can be null
     * @param <OUT>
     * @return
     */
    public <OUT> IPromise newPublisherServer(Publisher<OUT> source, ActorPublisher networkPublisher, Consumer<Actor> disconCB) {
        if (source instanceof PublisherActor == false) {
            Processor<OUT, OUT> proc = newProcessor(source, a -> a);
            source.subscribe(proc);
            source = proc;
        }
        return networkPublisher.facade((Actor) source).publish(disconCB);
    }

    /**
     * @param eventType
     * @param connectable
     * @param disconHandler - can be null
     * @param <T>
     * @return
     */
    public <T> IPromise<Publisher<T>> connectRemotePublisher(Class<T> eventType, ConnectableActor connectable, Callback<ActorClientConnector> disconHandler) {
        return connectable.actorClass(PublisherActor.class).inboundQueueSize(DEFAULTQSIZE).connect(disconHandler);
    }

    public <IN, OUT> Processor<IN, OUT> newProcessor(Publisher<IN> source, Function<IN, OUT> processingFunction) {
        return newProcessor(source, processingFunction, new SimpleScheduler(DEFAULTQSIZE), DEFAULT_BATCH_SIZE);
    }

    public <IN, OUT> Processor<IN, OUT> newProcessor(Publisher<IN> source, Function<IN, OUT> processingFunction, int batchSize ) {
        return newProcessor(source, processingFunction, new SimpleScheduler(DEFAULTQSIZE), batchSize);
    }

    public <IN, OUT> Processor<IN, OUT> newProcessor(Publisher<IN> source, Function<IN, OUT> processingFunction, Scheduler sched, int batchSize) {
        PublisherActor pub = Actors.AsActor(PublisherActor.class, sched);
        if ( batchSize > ReaktiveStreams.MAX_BATCH_SIZE ) {
            throw new RuntimeException("batch size exceeds max of "+ReaktiveStreams.MAX_BATCH_SIZE);
        }
        pub.setBatchSize(batchSize);
        pub.setProcessor(processingFunction);
        return pub;
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

        public MySubscriber(long batchSize, Callback<T> cb) {
            this.batchSize = batchSize;
            this.cb = cb;
            credits = 0;
        }

        @Override
        public void onSubscribe(Subscription s) {
            subs = s;
            s.request(batchSize);
            credits += batchSize;
        }

        @Override
        public void onNext(T t) {
            credits--;
            if ( credits < batchSize/ReaktiveStreams.REQU_NEXT_DIVISOR ) {
                subs.request(batchSize);
                credits += batchSize;
            }
            cb.stream(t);
        }

        @Override
        public void onError(Throwable t) {
            cb.reject(t);
        }

        @Override
        public void onComplete() {
            cb.finish();
        }
    }
}
