package org.nustaq.kontraktor.barebone;

/**
 * Created by ruedi on 05/06/15.
 */
public class Promise<T> implements Callback<T> {

    Callback<T> cb;
    T result;
    Object error;
    boolean hasFired = false;
    boolean isComplete = false;

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

    @Override
    public void receive(T result, Object error) {
        complete(result,error);
    }
}
