package de.ruedigermoeller.kontraktor;

import de.ruedigermoeller.kontraktor.Callback;
import de.ruedigermoeller.kontraktor.IFuture;
import de.ruedigermoeller.kontraktor.impl.CallbackWrapper;
import de.ruedigermoeller.kontraktor.impl.DispatcherThread;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by ruedi on 20.05.14.
 */
public class Future<T> implements IFuture<T> {

    protected Object result;
    protected Object error;
    protected Callback resultReceiver;
    protected boolean hadResult;
    protected boolean hasFired = false;
    // not necessary in many cases and also increases cost
    // of allocation. However for now stay safe and optimize
    // from a proven-working implementation
    AtomicBoolean lock = new AtomicBoolean(false);
    Thread currentThread;
    String id;
    IFuture nextFuture;

    public Future(T result, Object error) {
        currentThread = Thread.currentThread();
        this.result = result;
        this.error = error;
        hadResult = true;
    }

    public Future(T result) {
        this(result,null);
    }

    public Future() {
        currentThread = Thread.currentThread();
    }

    public String getId() {
        return id;
    }

    public Future<T> setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public IFuture then(Callback resultCB) {
        while( !lock.compareAndSet(false,true) ) {}
        resultReceiver = resultCB;
        if (hadResult) {
            if ( hasFired ) {
                lock.set(false);
                throw new RuntimeException("Double result received on future");
            }
            hasFired = true;
            lock.set(false);
            resultCB.receiveResult( result, error );
            nextFuture = new Future(result,error);
            return nextFuture;
        } else {
            lock.set(false);
        }
        if ( resultCB instanceof IFuture) {
            return (IFuture)resultCB;
        }
        return nextFuture = new Future();
    }

    @Override
    public void receiveResult(Object res, Object error) {
        // ensure correct thread in case actor cascades futures
        if ( Thread.currentThread() != currentThread && currentThread instanceof DispatcherThread) {
            new CallbackWrapper((DispatcherThread) currentThread, this).receiveResult(res, error);
        }
        else {
            this.result = res;
            this.error = error;
            while( !lock.compareAndSet(false,true) ) {}
            hadResult = true;
            if ( resultReceiver != null ) {
                if ( hasFired ) {
                    lock.set(false);
                    throw new RuntimeException("Double result received on future");
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

    @Override
    public String toString() {
        return "Result{" +
                "result=" + result +
                ", error=" + error +
                '}';
    }
}
