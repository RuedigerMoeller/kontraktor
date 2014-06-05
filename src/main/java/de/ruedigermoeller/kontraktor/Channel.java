package de.ruedigermoeller.kontraktor;

import de.ruedigermoeller.kontraktor.impl.CallbackWrapper;
import de.ruedigermoeller.kontraktor.impl.DispatcherThread;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by ruedi on 05.06.14.
 */
public class Channel<T> implements Future<T> {
    protected Queue result;
    protected Callback resultReceiver;
    Thread currentThread;
    String id;
    Future nextFuture;

    public Channel(T result) {
        currentThread = Thread.currentThread();
        this.result = new ConcurrentLinkedQueue<T>();
        this.result.offer(result);
    }

    public Channel() {
        currentThread = Thread.currentThread();
        this.result = new ConcurrentLinkedQueue<T>();
    }

    public String getId() {
        return id;
    }

    public Channel<T> setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public <OUT> Future<OUT> map(final Filter<T, OUT> filter) {
        final Promise<OUT> promise = new Promise<>();
        then(new Callback<T>() {
            @Override
            public void receiveResult(T result, Object error) {
                filter.map(result, error).then(promise);
            }
        });
        return promise;
    }

    @Override
    public Future then(Callback resultCB) {
        // FIXME: this can be implemented more efficient
        while( !lock.compareAndSet(false,true) ) {}
        resultReceiver = resultCB;
        if (hadResult) {
            if ( hasFired ) {
                lock.set(false);
                throw new RuntimeException("Double result received on future");
            }
            hasFired = true;
            lock.set(false);
            nextFuture = new Promise(result,error);
            resultCB.receiveResult( result, error );
            return nextFuture;
        } else {
            lock.set(false);
        }
        if ( resultCB instanceof Future) {
            return (Future)resultCB;
        }
        return nextFuture = new Promise();
    }

    @Override
    public final void receiveResult(Object res, Object error) {
        // ensure correct thread in case actor cascades futures
        if ( Thread.currentThread() != currentThread && currentThread instanceof DispatcherThread) {
            new CallbackWrapper((DispatcherThread) currentThread, this).receiveResult(res, error);
        }
        else {
            this.result = res;
            this.error = error;
            while( !lock.compareAndSet(false,true) ) {}
            if ( hadResult ) {
                lock.set(false);
                throw new RuntimeException("Double result received on future");
            }
            hadResult = true;
            if ( resultReceiver != null ) {
                if ( hasFired ) {
                    lock.set(false);
                    throw new RuntimeException("Double fire on callback");
                }
                hasFired = true;
                lock.set(false);
                resultReceiver.receiveResult(result,error);
                resultReceiver = null;
                if ( nextFuture != null )
                    nextFuture.receiveResult(result,error);
                return;
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

    // debug
    public boolean _isHadResult() {
        return hadResult;
    }

    // debug
    public boolean _isHasFired() {
        return hasFired;
    }

    @Override
    public String toString() {
        return "Result{" +
                "result=" + result +
                ", error=" + error +
                '}';
    }

}
