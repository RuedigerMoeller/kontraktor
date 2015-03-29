package org.nustaq.kontraktor;

/**
 * Created by ruedi on 26/03/15.
 *
 * only thrown in context of an all call
 *
 */
public class TimeoutException extends AwaitException {

    public TimeoutException() {
        super("Timeout");
    }
}
