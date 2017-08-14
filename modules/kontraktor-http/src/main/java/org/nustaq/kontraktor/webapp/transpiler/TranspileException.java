package org.nustaq.kontraktor.webapp.transpiler;

/**
 * Created by ruedi on 21.05.16.
 */
public class TranspileException extends RuntimeException {
    public TranspileException() {
    }

    public TranspileException(String message) {
        super(message);
    }

    public TranspileException(String message, Throwable cause) {
        super(message, cause);
    }

    public TranspileException(Throwable cause) {
        super(cause);
    }

    public TranspileException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
