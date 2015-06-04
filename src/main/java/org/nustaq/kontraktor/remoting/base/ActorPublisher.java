package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.IPromise;

/**
 * Configuration object to publish an actor to network. Serverside aequivalent to ConnectableActor
 */
public interface ActorPublisher {

    IPromise<ActorServer>  publish();

}
