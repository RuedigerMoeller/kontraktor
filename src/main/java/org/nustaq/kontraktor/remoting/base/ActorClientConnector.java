package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;

import java.util.function.Function;

/**
 * Created by ruedi on 10/05/15.
 */
public interface ActorClientConnector {
    /**
     * used in both client and server connector implementations
     */
    public static int OBJECT_MAX_BATCH_SIZE = 100;

    void connect(Function<ObjectSocket, ObjectSink> factory) throws Exception;
    IPromise closeClient();

}
