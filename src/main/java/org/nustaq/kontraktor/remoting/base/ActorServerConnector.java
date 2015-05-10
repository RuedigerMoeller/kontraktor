package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import java.util.function.Function;

/**
 * Created by ruedi on 09/05/15.
 */
public interface ActorServerConnector {

    void connect(Actor facade, Function<ObjectSocket, ObjectSink> factory) throws Exception;

    IPromise closeServer();
}
