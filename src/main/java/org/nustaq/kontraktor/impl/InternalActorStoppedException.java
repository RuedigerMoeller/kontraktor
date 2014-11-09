package org.nustaq.kontraktor.impl;

/**
 * Created by ruedi on 15.06.14.
 *
 * used to
 *
 */
public class InternalActorStoppedException extends RuntimeException {

    public static InternalActorStoppedException Instance = new InternalActorStoppedException();

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
