package org.nustaq.kontraktor;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by moelrue on 20.05.2014.
 */
public interface Future<T> extends Callback<T> {

    /**
     * called when any result of a future becomes available
     * Can be used in case a sender is not interested in the actual result
     * but when a remote method has finished processing.
     * @param result
     * @return
     */
    public Future<T> then( Runnable result );
    /**
     * called when any result of a future becomes available
     * @param result
     * @return
     */
    public Future<T> then( Callback<T> result );
    public Future<T> then( Supplier<Future<T>> result );
    public <OUT> Future<OUT> then(final Function<T, Future<OUT>> function);
    public <OUT> Future<OUT> then(final Consumer<T> function);
    public <OUT> Future<OUT> catchError(final Function<Object, Future<OUT>> function);
    public <OUT> Future<OUT> catchError(final Consumer<Object> function);

    /**
     * called when a valid result of a future becomes available.
     * forwards to (new) "then" variant.
     * @return
     */
    default public Future<T> onResult( Consumer<T> resultHandler ) {
        return then(resultHandler);
    }

    /**
     * called when an error is set as the result
     * forwards to (new) "catchError" variant.
     * @return
     */
    default public Future<T> onError( Consumer<Object> errorHandler ) {
        return catchError(errorHandler);
    }

    /**
     * called when the async call times out. see 'timeOutIn'
     * @param timeoutHandler
     * @return
     */
    public Future<T> onTimeout(Consumer timeoutHandler);

    /**
     * @return result if avaiable
     */
    public T getResult();

    /**
     * @return error if avaiable
     */
    public Object getError();

    /**
     * tell the future to call the onTimeout callback in N milliseconds if future is not settled until then
     *
     * @param millis
     * @return this for chaining
     */
    public Future timeoutIn(long millis);
}
