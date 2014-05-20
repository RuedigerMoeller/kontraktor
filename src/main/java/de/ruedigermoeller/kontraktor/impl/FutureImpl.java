package de.ruedigermoeller.kontraktor.impl;

import de.ruedigermoeller.kontraktor.Callback;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by ruedi on 20.05.14.
 */
public class FutureImpl<T> implements Future<T> {

    protected Object result;             // FIXME: volatile required for both ?
    protected Callback resultReceiver;
    AtomicBoolean lock = new AtomicBoolean(false);

    public FutureImpl() {
    }

    public FutureImpl(Throwable exception) {
        receiveResult(exception, WRONG);
    }

    public FutureImpl(T result) {
        receiveResult(result, WRONG);
    }

    public void receiveResult(Object res, Object error) {
        while( ! lock.compareAndSet(false,true) ) {
        }
        try {
            if (resultReceiver != null) {
                boolean ex = result instanceof Throwable;
                resultReceiver.receiveResult(ex ? null:res, ex ? res: null );
            }
            if (res == null)
                res = CallEntry.NULL_RESULT;
            this.result = res;
        } finally {
            lock.set(false);
        }
    }

    @Override
    public void then(Callback resultCB) {
        while( ! lock.compareAndSet(false,true) ) {
        }
        try {
            if (result != null) {
                boolean ex = result instanceof Throwable;
                resultCB.receiveResult(ex ? null:result, ex ? result: null );
            }
            resultReceiver = new CallbackWrapper(DispatcherThread.getThreadDispatcher(), resultCB);
        } finally {
            lock.set(false);
        }
    }
}
