package org.nustaq.kontraktor.impl;

/**
 * Created by ruedi on 30.10.14.
 *
 * if an actor is blocked because a receiver's queue is full
 */
public class ActorBlockedException extends RuntimeException {

    public static ActorBlockedException Instance = new ActorBlockedException();
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

}
