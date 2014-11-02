package org.nustaq.kontraktor.impl;

/**
 * Created by ruedi on 02.11.14.
 */
public class StoppedActorTargetedException extends RuntimeException {
    public StoppedActorTargetedException() {
    }

    public StoppedActorTargetedException(String message) {
        super(message);
    }

    public StoppedActorTargetedException(String message, Throwable cause) {
        super(message, cause);
    }

    public StoppedActorTargetedException(Throwable cause) {
        super(cause);
    }

    public StoppedActorTargetedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
