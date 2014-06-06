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
    public T getResult() {
        return (T) result.peek(); // fixme: wrong
    }

    @Override
    public Future then(Callback resultCB) {
        // FIXME: this can be implemented more efficient
        resultReceiver = resultCB;
        if ( resultCB instanceof Future) {
            return (Future)resultCB;
        }
        return nextFuture = new Promise();
    }

    private void poll() {
        if ( resultReceiver != null )
            return;
        Object poll = null;
        while ((poll=result.poll())!=null) {
            resultReceiver.receiveResult(poll, null);
        }
    }

    @Override
    public final void receiveResult(Object res, Object error) {
        result.offer(res);
        poll();
    }

    public Object getError() {
        return null;
    }

    @Override
    public String toString() {
        return "Result{" +
                "result=" + result.size() +
                '}';
    }

}
