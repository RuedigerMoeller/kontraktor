package de.ruedigermoeller.kontraktor;

/**
 * Created by ruedi on 06.05.14.
 *
 * allow for callbacks using JDK 8 lambdas. Callback is an abstract class
 * for performance reasons.
 */
public class LambdaCB<T> extends Callback<T> {

    public static interface CB<T> {
        void result( T result, Object error );
    }

    final CB callback;

    public LambdaCB(CB<T> callback) {
        this.callback = callback;
    }

    @Override
    public void receiveResult(T result, Object error) {
        callback.result(result,error);
    }
}
