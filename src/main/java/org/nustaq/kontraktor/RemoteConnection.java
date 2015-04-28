package org.nustaq.kontraktor;


/**
 * Created by ruedi on 24.08.2014.
 */
public interface RemoteConnection {
    /**
     * closes the underlying connection (Warning: may side effect to other actors published on this connection)
     */
    public void close();
    public void setClassLoader( ClassLoader l );
    public int getRemoteId( Actor act );

    /**
     * unpublishes this actor by removing mappings and stuff. Does not actively close the underlying connection
     * @param self
     *
     */
    public void unpublishActor(Actor self);
}
