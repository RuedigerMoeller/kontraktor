package de.ruedigermoeller.kontraktor;

import java.util.*;

/**
 * Created by moelrue on 14.05.2014.
 */
public class KFuture<T> {

    T result;
    Object error;
    volatile Callback<T> callback;

    public KFuture(T result) {
        this( result, null );
    }

    public KFuture(T result, Object error) {
        this.result = result;
        this.error = error;
    }

    public void receive( T result, Object error ) {
        if ( callback != null ) {
            callback.receiveResult(result,error);
        } else {
            this.result = result;
            this.error = error;
            then(callback);
        }
    }

    public void then( Callback<T> callback ) {
        this.callback = Actors.InThread(callback);
        if ( callback != null && (result != null || error != null ) ) {
            result = null; error = null;
            callback.receiveResult(result, error);
        }
    }

}
