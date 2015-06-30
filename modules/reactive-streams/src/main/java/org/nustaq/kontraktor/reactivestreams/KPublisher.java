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
public interface KPublisher<T> extends Publisher<T> {

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
     * @param
     */
    @CallerSideMethod default void subscribe( Callback<T> cb ) {
        subscribe(ReaktiveStreams.get().subscriber(cb));
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
     */
    @CallerSideMethod default void subscribe( int batchSize, Callback<T> cb ) {
        subscribe(ReaktiveStreams.get().subscriber(batchSize, cb));
    }

    @CallerSideMethod default <OUT> KPublisher<OUT> asyncMap(Function<T,OUT> processor) {
        Processor<T, OUT> toutProcessor = ReaktiveStreams.get().newAsyncProcessor(this, processor);
        subscribe(toutProcessor);
        return (KPublisher<OUT>) toutProcessor;
    }

    @CallerSideMethod default <OUT> KPublisher<OUT> asyncMap( Function<T,OUT> processor, int batchSize ) {
        Processor<T, OUT> toutProcessor = ReaktiveStreams.get().newAsyncProcessor(this, processor, batchSize);
        subscribe(toutProcessor);
        return (KPublisher<OUT>) toutProcessor;
    }

    @CallerSideMethod default ActorServer publish( ActorPublisher publisher, Consumer<Actor> disconCallback ) {
        return (ActorServer) ReaktiveStreams.get().newPublisherServer(this, publisher, disconCallback).await();
    }

    @CallerSideMethod default <OUT> KPublisher<OUT> map(Function<T,OUT> processor) {
        return null;
//        return (KPublisher<OUT>) new ReaktiveStreams.SyncPublisher(ReaktiveStreams.DEFAULT_BATCH_SIZE, null, false ) {
//            @Override
//            protected void nextAction(Object o) {
//
//            }
//        };
    }

}
