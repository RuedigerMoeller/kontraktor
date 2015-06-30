package org.nustaq.kontraktor.reactivestreams;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by ruedi on 28/06/15.
 */
public class EventSink<T> implements KPublisher<T> {

    protected AtomicLong credits = new AtomicLong(0);
    protected volatile Subscriber subs;

    public boolean offer(T event) {
        if ( event == null )
            throw new RuntimeException("event cannot be null");
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
