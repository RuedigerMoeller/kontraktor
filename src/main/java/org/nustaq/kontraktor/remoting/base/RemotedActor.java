package org.nustaq.kontraktor.remoting.base;

/**
 * optional interface implementing some notification callbacks
 * related to remoting.
 */
public interface RemotedActor {

    /**
     * notification method called once an actor has been unpublished. E.g. in case of a ClientSession role
     * actor, this will be called once the client disconnects or times out
     */
    public void hasBeenUnpublished();

}
