package org.nustaq.kontraktor;

/**
 * Created by ruedi on 26/03/15.
 *
 * only thrown in context of a all call
 *
 */
public class TimeoutException extends YieldException {

    public TimeoutException() {
        super("Timeout");
    }
}
