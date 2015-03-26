package org.nustaq.kontraktor;

/**
 * Created by moelrue on 3/26/15.
 */
public class YieldException extends RuntimeException {
    Object o;

    public YieldException(Object o) {
        this.o = o;
    }

}
