package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;

import java.util.function.Function;

/**
 * Interface unifying remote actor connectors (the thingy translating local calls to remote calls).
 * Mostly of internal interest.
 */
public interface ActorClientConnector {

    /**
     * used in most client and server connector implementations
     */
    public static int OBJECT_MAX_BATCH_SIZE = 100;

    IPromise connect(Function<ObjectSocket, ObjectSink> factory) throws Exception;
    IPromise closeClient();

}
