package org.nustaq.kontraktor.barebone;

/**
 * Created by ruedi on 05/06/15.
 */
public class BBPromise<T> {

    volatile BBPromise cb;

    public void complete( T result, Object error ) {
    }

    public void then( BBPromise<T> callback ) {
        cb = callback;
    }

}
