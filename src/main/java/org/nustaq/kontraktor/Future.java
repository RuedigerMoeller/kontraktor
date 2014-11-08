package org.nustaq.kontraktor;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by moelrue on 20.05.2014.
 */
public interface Future<T> extends Callback<T> {
    public Future<T> then( Runnable result );
    public Future<T> then( Callback<T> result );
    public Future<T> onResult( Consumer<T> resultHandler );
    public Future<T> onError( Consumer errorHandler );
    public Future<T> onTimeout(Consumer timeoutHandler);
    public <OUT> Future<OUT> map(final Filter<T, OUT> filter);

    /**
     * @return result if already avaiable
     */
    public T getResult();
    public Object getError();
    /**
     * same as receive(null,null)
     */
    public void signal();

    /**
     *
     * @param millis
     * @return this for chaining
     */
    public Future timeoutIn(long millis);
}
