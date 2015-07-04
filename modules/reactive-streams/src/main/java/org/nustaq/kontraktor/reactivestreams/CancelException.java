package org.nustaq.kontraktor.reactivestreams;

/**
 * Created by ruedi on 04/07/15.
 *
 * Can be used to signal a cancel subscription from a kontraktor callback
 *
 */
public class CancelException extends RuntimeException {

    public static final CancelException Instance = new CancelException();

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

}
