package org.nustaq.kontraktor.reactivestreams;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.remoting.base.ActorPublisher;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by ruedi on 30/06/15.
 */
public interface KPublisher<T> extends Publisher<T>, KontraktorChain {

    /**
     * consuming endpoint. requests data from publisher immediately after
     * receiving onSubscribe callback. Maps from reactive streams Subscriber to Kontraktor Callback.
     *
     * Call inherited subscribe(Publisher) to subscribe a 'standard' reactivestreams subscriber
     *
     * all events are routed to the callback
     * e.g.
     * <pre>
     *      subscriber( (event, err) -> {
     +          if (Actors.isErrorOrComplete(err)) {
     +              System.out.println("complete");
     +          } else if (Actors.isError(err)) {
     +              System.out.println("ERROR");
     +          } else {
     +              // process event
     +          }
     +      }
     * </pre>
     *
     * @param
     */
    @CallerSideMethod default void subscribe( Callback<T> cb ) {
        subscribe(ReaktiveStreams.get().subscriber(cb));
    }

    /**
     * consuming endpoint. requests data from publisher immediately after
     * receiving onSubscribe callback. Maps from reactive streams Subscriber to Kontraktor Callback
     *
     * Call inherited subscribe(Publisher) to subscribe a 'standard' reactivestreams subscriber
     *
     * all events are routed to the callback
     * e.g.
     * <pre>
     *      subscriber( (event, err) -> {
     +          if (Actors.isErrorOrComplete(err)) {
     +              System.out.println("complete");
     +          } else if (Actors.isError(err)) {
     +              System.out.println("ERROR");
     +          } else {
     +              // process event
     +          }
     +      }
     * </pre>
     *
     */
    @CallerSideMethod default void subscribe( int batchSize, Callback<T> cb ) {
        subscribe(ReaktiveStreams.get().subscriber(batchSize, cb));
    }

    /**
     * insert an async processor (with dedicated thread)
     *
     * @param processor
     * @param <OUT>
     * @return
     */
    @CallerSideMethod default <OUT> KPublisher<OUT> asyncMap(Function<T,OUT> processor) {
        Processor<T, OUT> toutProcessor = ReaktiveStreams.get().newAsyncProcessor(processor);
        subscribe(toutProcessor);
        return (KPublisher<OUT>) toutProcessor;
    }

    /**
     * insert an async processor (with dedicated thread)
     *
     * @param processor
     * @param batchSize
     * @param <OUT>
     * @return
     */
    @CallerSideMethod default <OUT> KPublisher<OUT> asyncMap( Function<T,OUT> processor, int batchSize ) {
        Processor<T, OUT> toutProcessor = ReaktiveStreams.get().newAsyncProcessor(processor, batchSize);
        subscribe(toutProcessor);
        return (KPublisher<OUT>) toutProcessor;
    }

    /**
     * insert an async processor (with dedicated thread)
     *
     * @param publisher
     * @param disconCallback
     * @return
     */
    @CallerSideMethod default ActorServer publish( ActorPublisher publisher, Consumer<Actor> disconCallback ) {
        return (ActorServer) ReaktiveStreams.get().newPublisherServer(this, publisher, disconCallback).await();
    }

    /**
     * insert a synchronous processor (runs in provider thread).
     * if 'this' is a remote reference to a remote stream, a queued async processor will be
     * created (need queue + processing thread then)
     *
     * @param processor
     * @param <OUT>
     * @return
     */
    @CallerSideMethod default <OUT> KPublisher<OUT> map(Function<T,OUT> processor) {
        if ( this instanceof PublisherActor && ((PublisherActor)this).isRemote() )
            return asyncMap(processor); // need a queue when connecting remote stream
        Processor<T,OUT> outkPublisher = ReaktiveStreams.get().newSyncProcessor(processor);
        subscribe(outkPublisher);
        return (KPublisher<OUT>) outkPublisher;
    }

}
