/*
 * Copyright 2014 Ruediger Moeller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nustaq.kontraktor.barebone;


import org.nustaq.serialization.util.FSTUtil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by ruedi on 05/06/15.
 *
 * A minimalistic Promise (~CompletableFuture) implementation using simple lock based synchronization
 */
public class Promise<T> extends Callback<T> {

    Callback<T> cb;
    T result;
    Object error;
    boolean hasFired = false;
    boolean isComplete = false;

    /**
     * shorthand for complete( result, null )
     * @param result
     */
    public void resolve( T result ) {
        complete(result,null);
    }

    /**
     * shorthand for complete( null, error)
     * @param error
     */
    public void reject( Object error ) {
        complete(null,error);
    }

    /**
     * to be called by code returning an result or error.
     *
     * @param result
     * @param error
     */
    public void complete( T result, Object error ) {
        synchronized (this) {
            if ( isComplete ) {
                throw new RuntimeException("Promise can be completed only once");
            }
            this.result = result;
            this.error = error;
            isComplete = true;
            tryFire();
        }
    }

    private void tryFire() {
        synchronized (this) {
            if ( cb != null && isComplete && ! hasFired ) {
                hasFired = true;
                cb.receive(result, error);
            }
        }
    }

    public void then( Callback<T> callback ) {
        synchronized (this) {
            if ( cb != null )
                throw new RuntimeException("only one callback can be used");
            cb = callback;
        }
        tryFire();
    }

    /**
     * to be called by code returning an result or error.
     * @param result
     * @param error
     */
    @Override
    public void receive(T result, Object error) {
        complete(result,error);
    }

    /**
     *  see await( long timeout )
     */
    public T await() {
        return await(5000);
    }

    /**
     * in contradiction to original kontraktor promise this is blocking as there are no "Actors" in the client :).
     * awaits makes a synchronous method out of a async Promise-returning call. If the promise resolves to an error,
     * an exceptions is thrown, else the result is returned.
     * @return
     */
    public T await(long timeout) {
        final CountDownLatch latch = new CountDownLatch(1);
        cb = new Callback<T>() {
            @Override
            public void receive(T result, Object error) {
                latch.countDown();
            }
        };
        boolean timedOut = true;
        try {
            timedOut = !latch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if ( timedOut )
            throw new RuntimeException("promise timed out");
        if ( error instanceof Throwable ) {
            FSTUtil.<RuntimeException>rethrow(((Throwable) error));
        } else if ( error != null ) {
            throw new RuntimeException(""+error);
        }
        return result;
    }

}
