package de.ruedigermoeller.kontraktor.impl;

import de.ruedigermoeller.kontraktor.Callback;
import de.ruedigermoeller.kontraktor.Future;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by ruedi on 20.05.14.
 */
public class Result<T> implements Future<T> {

    protected Object result;
    protected Object error;
    protected Callback resultReceiver;
    protected boolean hadResult;
    // not necessary in many cases and also increases cost
    // of allocation. However for now stay safe and optimize
    // from a proven-working implementation
    AtomicBoolean lock = new AtomicBoolean(false);
    Thread currentThread;
    String id;

    public Result(T result, Object error) {
        currentThread = Thread.currentThread();
        this.result = result;
        this.error = error;
        hadResult = true;
    }

    public Result(T result) {
        this(result,null);
    }

    public Result() {
        currentThread = Thread.currentThread();
    }

    public String getId() {
        return id;
    }

    public Result<T> setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public void then(Callback resultCB) {
        while( !lock.compareAndSet(false,true) ) {}
        resultReceiver = resultCB;
        if (hadResult) {
            lock.set(false);
            resultCB.receiveResult( result, error );
            return;
        } else {
            lock.set(false);
        }
    }

    @Override
    public void receiveResult(Object res, Object error) {
        // ensure correct thread in case actor cascades futures
        if ( Thread.currentThread() != currentThread && currentThread instanceof DispatcherThread ) {
            new CallbackWrapper((DispatcherThread) currentThread, this).receiveResult(res, error);
        }
        else {
            this.result = res;
            this.error = error;
            while( !lock.compareAndSet(false,true) ) {}
            hadResult = true;
            if ( resultReceiver != null ) {
                lock.set(false);
                resultReceiver.receiveResult(result,error);
                resultReceiver = null;
            } else {
                lock.set(false);
            }
        }
    }

    public T getResult() {
        return (T) result;
    }

    public Object getError() {
        return error;
    }
}
