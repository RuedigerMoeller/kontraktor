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

    ////////// singleton instance methods ////////////////////////////////////////////////

    /**
     * consuming endpoint. requests data from publisher immediately after
     * receiving onSubscribe callback
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
     *      subscriber( (event, err) -> {
     +          if (Actors.isFinal(err)) {
     +              System.out.println("complete");
     +          } else if (Actors.isError(err)) {
     +              System.out.println("ERROR");
     +          } else {
     +              // process event
     +          }
     +      }
     * </pre>
     *
     * @param <T>
     */
    public <T> Subscriber<T> subscriber(int batchSize, Callback<T> cb) {
        if ( batchSize > MAX_BATCH_SIZE ) {
            throw new RuntimeException("batch size exceeds maximum of "+MAX_BATCH_SIZE);
        }
        return new MySubscriber<>(batchSize, cb);
    }

    public <T> KPublisher<T> connect( Class eventType, ConnectableActor connectable ) {
        return (KPublisher<T>) get().connectRemotePublisher(eventType, connectable, null ).await();
    }

    public <T> KPublisher<T> connect( Class eventType, ConnectableActor connectable, Callback<ActorClientConnector> disconCB ) {
        return (KPublisher<T>) get().connectRemotePublisher(eventType, connectable, disconCB).await();
    }

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
        if (source instanceof PublisherActor == false || source instanceof ActorProxy == false ) {
            Processor<OUT, OUT> proc = newAsyncProcessor(source, a -> a); // we need a queue before going to network
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
    public <T> IPromise<KPublisher<T>> connectRemotePublisher(Class<T> eventType, ConnectableActor connectable, Callback<ActorClientConnector> disconHandler) {
        return connectable.actorClass(PublisherActor.class).inboundQueueSize(DEFAULTQSIZE).connect(disconHandler);
    }

    public <IN, OUT> Processor<IN, OUT> newAsyncProcessor(Publisher<IN> source, Function<IN, OUT> processingFunction) {
        return newAsyncProcessor(source, processingFunction, new SimpleScheduler(DEFAULTQSIZE), DEFAULT_BATCH_SIZE);
    }

    public <IN, OUT> Processor<IN, OUT> newAsyncProcessor(Publisher<IN> source, Function<IN, OUT> processingFunction, int batchSize) {
        return newAsyncProcessor(source, processingFunction, new SimpleScheduler(DEFAULTQSIZE), batchSize);
    }

    public <IN, OUT> Processor<IN, OUT> newAsyncProcessor(Publisher<IN> source, Function<IN, OUT> processingFunction, Scheduler sched, int batchSize) {
        PublisherActor pub = Actors.AsActor(PublisherActor.class, sched);
        if ( batchSize > ReaktiveStreams.MAX_BATCH_SIZE ) {
            throw new RuntimeException("batch size exceeds max of "+ReaktiveStreams.MAX_BATCH_SIZE);
        }
        pub.setBatchSize(batchSize);
        pub.setProcessor(processingFunction);
        return pub;
    }

    public <IN,OUT> Processor<IN,OUT> newSyncProcessor(Publisher<IN> source, Function<IN, OUT> processingFunction) {
        PublisherActor pub = new PublisherActor(); // use actor as normal class here (=sync)
        pub.__self = pub;
        pub.setBatchSize(DEFAULT_BATCH_SIZE);
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
            subs = s;
            if ( autoRequestOnSubs )
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
            nextAction(t);
        }

        protected void nextAction(T t) {
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

    protected static class SyncProcessor<IN, OUT> implements Processor<IN, OUT>, KPublisher<OUT> {

        @Override
        public void subscribe(Subscriber<? super OUT> s) {

        }

        @Override
        public void onSubscribe(Subscription s) {

        }

        @Override
        public void onNext(IN in) {

        }

        @Override
        public void onError(Throwable t) {

        }

        @Override
        public void onComplete() {

        }
    }
}
