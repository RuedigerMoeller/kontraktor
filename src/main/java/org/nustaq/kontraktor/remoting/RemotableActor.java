package org.nustaq.kontraktor.remoting;

/**
 * optional interface implementing some notification callbacks
 * related to remoting.
 */
public interface RemotableActor {

    public void $hasBeenUnpublished(RemotedActorMappingSnapshot snapshot);

}
