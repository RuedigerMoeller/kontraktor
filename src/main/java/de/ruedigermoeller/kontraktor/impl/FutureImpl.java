package de.ruedigermoeller.kontraktor.impl;

import de.ruedigermoeller.kontraktor.Callback;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by ruedi on 20.05.14.
 */
public class FutureImpl<T> implements Future<T> {

    protected Object result;
    protected Object error;
    private Callback resultReceiver;

    public FutureImpl() {
    }

    public FutureImpl(Throwable exception) {
        receiveResult(null, exception);
    }

    public FutureImpl(T result) {
        receiveResult(result, null);
    }

    public void receiveResult(Object res, Object error) {
        result = res;
        error = error;
        if ( resultReceiver != null ) {
            resultReceiver.receiveResult(result,error);
            resultReceiver = null;
        }
    }

    @Override
    public void then(Callback resultCB) {
        if (result != null) {
            boolean ex = result instanceof Throwable;
            resultCB.receiveResult(ex ? null:result, ex ? result: null );
            return;
        }
        resultReceiver = resultCB;
    }
}
