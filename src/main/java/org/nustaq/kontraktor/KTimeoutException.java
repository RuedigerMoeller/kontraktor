package org.nustaq.kontraktor;

/**
 * Created by ruedi on 26/03/15.
 *
 * only thrown in context of an await call
 *
 */
public class KTimeoutException extends AwaitException {

    public KTimeoutException() {
        super("Timeout");
    }
}
