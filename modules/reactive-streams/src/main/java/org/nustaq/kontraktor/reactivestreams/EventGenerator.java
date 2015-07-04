package org.nustaq.kontraktor.reactivestreams;

import org.nustaq.kontraktor.Actor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Created by ruedi on 04/07/15.
 */
public class EventGenerator<T> implements KPublisher<T> {

    public static <T> EventGenerator<T> of( Stream<T> stream ) {
        return new EventGenerator<T>(stream);
    }

    public static <T> EventGenerator<T> of( Iterator<T> it ) {
        return new EventGenerator<T>(it);
    }

    // FIXME: some cut & paste from eventsink ..

    protected AtomicLong credits = new AtomicLong(0);
    protected volatile Subscriber subs;

    protected Iterator<T> iterator;
    protected boolean complete = false;

    public EventGenerator(Stream<T> stream) {
        this.iterator = stream.iterator();
    }

    public EventGenerator(Iterator<T> iterator) {
        this.iterator = iterator;
    }

    public void complete() {
        subs.onComplete();
    }

    public void error( Throwable th ) {
        subs.onError(th);
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        if ( subscriber == null )
            throw null;
        if ( subs != null ) {
            throw new RuntimeException("only one subscription supported");
        }
        subs = subscriber;
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long l) {
                if ( l <= 0 ) {
                    subs.onError(new IllegalArgumentException("spec rule 3.9: request > 0 elements"));
                }
                credits.addAndGet(l);
                while( !complete && credits.getAndDecrement() > 0 ) {
                    if ( iterator.hasNext() ) {
                        try {
                            subs.onNext(iterator.next());
                        } catch (Throwable t) {
                            subs.onError(t);
                            complete = true;
                        }
                    } else {
                        subs.onComplete();
                        complete = true;
                        break;
                    }
                }
            }

            @Override
            public void cancel() {
                subs = null;
            }
        });
    }

    @Override
    public void sourceStopped() {
        if ( subs instanceof KontraktorChain )
            ((KontraktorChain) subs).sourceStopped();
    }

}
