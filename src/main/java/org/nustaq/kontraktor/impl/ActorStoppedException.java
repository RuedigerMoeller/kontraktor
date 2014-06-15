package org.nustaq.kontraktor.impl;

import sun.security.jca.GetInstance;

/**
 * Created by ruedi on 15.06.14.
 */
public class ActorStoppedException extends RuntimeException {

    public static ActorStoppedException Instance = new ActorStoppedException();

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
