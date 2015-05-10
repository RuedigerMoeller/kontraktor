package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;

import java.util.function.Function;

/**
 * Created by ruedi on 10/05/15.
 */
public interface ActorClientConnector {

    void connect(Function<ObjectSocket, ObjectSink> factory) throws Exception;
    IPromise closeClient();

}
