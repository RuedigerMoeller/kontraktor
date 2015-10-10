/*
Kontraktor Copyright (c) Ruediger Moeller, All rights reserved.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3.0 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

See https://www.gnu.org/licenses/lgpl.txt
*/

package org.nustaq.kontraktor;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * IPromise interface. Promise is another term for Future, to avoid naming clashes with JDK.
 * The only implementation is "Promise" currently. I try to stick
 * to ES6/7 terminology where possible.
 */
public interface IPromise<T> extends Callback<T> {

    /**
     * called once any result of a future becomes available
     * Can be used in case a sender is not interested in the actual result
     * but when a remote method has finished processing.
     *
     * e.g. actor.asyncMehod().then( () -> furtherProcessing() );
     *
     * @return a future ressolved after this
     */
    public IPromise<T> then( Runnable result );

    /**
     * called once any result of a future becomes available
     * Can be used in case a sender is not interested in the actual result
     * but when a remote method has finished processing.
     *
     * e.g. actor.asyncMehod().then( () -> furtherProcessing() );
     *
     * @return a future ressolved with the Callable result after this
     */
    public IPromise<T> then( Callback<T> result );

    /**
     * called once any result of a future becomes available
     * Can be used in case a sender is not interested in the actual result
     * but when a remote method has finished processing.
     *
     * e.g. actor.asyncMehod().then( () -> { furtherProcessing(); return new Promise("result"); } );
     *
     * @return a future ressolved with the Supüplier result after this
     */
    public IPromise<T> thenAnd(Supplier<IPromise<T>> result);

    /**
     * called once any result of a future becomes available
     * Can be used in case a sender is not interested in the actual result
     * but when a remote method has finished processing.
     *
     * e.g. actor.asyncMehod().then( () -> { furtherProcessing(); return new Promise("result"); } );
     *
     * @return a future ressolved with the Function result after this
     */
    public <OUT> IPromise<OUT> thenAnd(final Function<T, IPromise<OUT>> function);

    /**
     * called once any result of a future becomes available
     * Can be used in case a sender is not interested in the actual result
     * but when a remote method has finished processing.
     *
     * e.g. actor.asyncMehod().then( () -> { furtherProcessing(); return new Promise("result"); } );
     *
     * @return a future ressolved empty after this
     */
    public <OUT> IPromise<OUT> then(final Consumer<T> function);

    /**
     * called if an error has been signaled by one of the futures in the previous future chain.
     *
     * e.e. actor.async().then( ).then( ).then( ).catchError( error -> .. );
     */
    public <OUT> IPromise<OUT> catchError(final Function<Object, IPromise<OUT>> function);

    /**
     * called if an error has been signaled by one of the futures in the previous future chain.
     *
     * e.e. actor.async().then( ).then( ).then( ).catchError( () -> .. );
     */
    public <OUT> IPromise<OUT> catchError(final Consumer<Object> function);

    /**
     * called when a valid result of a future becomes available.
     * forwards to (new) "then" variant.
     * @return
     */
    default public IPromise<T> onResult( Consumer<T> resultHandler ) {
        return then(resultHandler);
    }

    /**
     * called when an error is set as the result
     * forwards to (new) "catchError" variant.
     * @return
     */
    default public IPromise<T> onError( Consumer<Object> errorHandler ) {
        return catchError(errorHandler);
    }

    /**
     * called when the async call times out. see 'timeOutIn'
     * @param timeoutHandler
     * @return
     */
    IPromise<T> onTimeout(Consumer timeoutHandler);

    default IPromise<T> onTimeout(Runnable timeoutHandler) {
        return onTimeout( to -> timeoutHandler.run() );
    }

    /**
     * Warning: this is different to JDK's BLOCKING future
     * @return result if avaiable (no blocking no awaiting).
     */
    public T get();

    /**
     * schedule other events/messages until future is resolved/settled (Nonblocking wait).
     *
     * In case this is called from a non-actor thread, the current thread is blocked
     * until the result is avaiable.
     *
     * If the future is rejected (resolves to an error) an exception is raised.
     *
     * This method is aequivalent to await(15000) = 15 seconds timeout. use await(0) to wait infinetely.
     *
     * @return the futures result or throw exception in case of error
     */
    default public T await() {
        return await(Actors.DEFAULT_TIMOUT);
    }

    /**
     * schedule other events/messages until future is resolved/settled (Nonblocking delay).
     *
     * In case this is called from a non-actor thread, the current thread is blocked
     * until the result is avaiable.
     *
     * If the future is rejected (resolves to an error) an excpetion is raised.
     *
     * if timeout is 0l - wait forever.
     *
     * @return the futures result or throw exception in case of error
     */
    public T await(long timeoutMillis);

    /**
     * schedule other events/messages until future is resolved/settled (Nonblocking delay).
     *
     * In case this is called from a non-actor thread, the current thread is blocked
     * until the result is avaiable.
     *
     * @return the settled promise. No Exception is thrown, but the exception can be obtained by IPromise.getError()
     * @param timeout
     */
    public IPromise<T> awaitPromise(long timeout);

    /**
     * similar to await but does not unwrap the promise. So a fulfilled promise is returned.
     * In case of errors, no exception is thrown to the caller but the promise error is set.
     * @return
     */
    default public IPromise<T> awaitPromise() {
        return awaitPromise(0);
    }

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
    public IPromise<T> timeoutIn(long millis);

    /**
     * @return wether an error or a result has been set to this future
     */
    public boolean isSettled();

}
