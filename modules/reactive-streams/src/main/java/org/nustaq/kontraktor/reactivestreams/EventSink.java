package org.nustaq.kontraktor.reactivestreams;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.base.ActorPublisher;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by ruedi on 28/06/15.
 */
public class EventSink<T> implements Publisher<T> {

    protected AtomicLong credits = new AtomicLong(0);
    protected volatile Subscriber subs;

    public boolean offer(T event) {
        if ( credits.get() > 0 ) {
            subs.onNext(event);
            credits.decrementAndGet();
            return true;
        }
        return false;
    }

    public void complete() {
        subs.onComplete();
    }

    public void error( Throwable th ) {
        subs.onError(th);
    }

    public <OUT> Processor<T,OUT> map( Function<T,OUT> processor) {
        Processor<T, OUT> toutProcessor = ReaktiveStreams.get().newProcessor(this, processor);
        subscribe(toutProcessor);
        return toutProcessor;
    }

    public <OUT> Processor<T,OUT> map( Function<T,OUT> processor, int batchSize ) {
        Processor<T, OUT> toutProcessor = ReaktiveStreams.get().newProcessor(this, processor, batchSize);
        subscribe(toutProcessor);
        return toutProcessor;
    }

    public EventSink publish( ActorPublisher publisher, Consumer<Actor> disconCB ) {
        ReaktiveStreams.get().newPublisherServer(this, publisher, disconCB).await();
        return this;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        if ( subs != null ) {
            throw new RuntimeException("only one subscription supported");
        }
        subs = subscriber;
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long l) {
                credits.addAndGet(l);
            }

            @Override
            public void cancel() {
                subs = null;
            }
        });
    }
}
