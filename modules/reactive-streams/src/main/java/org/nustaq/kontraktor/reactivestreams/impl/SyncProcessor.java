package org.nustaq.kontraktor.reactivestreams.impl;

import org.nustaq.kontraktor.reactivestreams.KxPublisher;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.function.Function;

/**
 * Created by ruedi on 06/07/15.
 */
public class SyncProcessor<IN, OUT> implements Processor<IN, OUT>, KxPublisher<OUT> {

    protected Subscription inSubs;
    protected Subscription outSubs;
    protected Subscriber<OUT> subscriber;
    protected boolean done = false;
    protected long batchSize;
    protected Function<IN, OUT> proc;
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
        if (initialRequest > 0)
            s.request(initialRequest);
    }

    @Override
    public void onNext(final IN element) {
        if (done)
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
        if (subscriber != null)
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
        if (subscriber != null) {
            throw new RuntimeException("can only subscribe once");
        }
        subscriber = (Subscriber<OUT>) s;
        s.onSubscribe(outSubs = new MySubs());
    }

    protected class MySubs implements Subscription {
        @Override
        public void request(long n) {
            if (n <= 0) {
                subscriber.onError(new IllegalArgumentException("rule 3.9: request > 0 elements"));
                return;
            }
            if (inSubs != null)
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
