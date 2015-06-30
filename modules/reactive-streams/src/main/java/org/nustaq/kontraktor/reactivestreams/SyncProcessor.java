package org.nustaq.kontraktor.reactivestreams;

import org.reactivestreams.*;

import java.util.function.*;

/**
 * Created by moelrue on 30.06.2015.
 */
public class SyncProcessor<IN,OUT> implements Processor<IN,OUT> {

    Subscriber<IN> source;
    Function<IN,OUT> proc;

    @Override
    public void subscribe(Subscriber<? super OUT> s) {

    }

    @Override
    public void onSubscribe(Subscription s) {

    }

    @Override
    public void onNext(IN in) {
        proc.apply(in);
    }

    @Override
    public void onError(Throwable t) {

    }

    @Override
    public void onComplete() {

    }

}
