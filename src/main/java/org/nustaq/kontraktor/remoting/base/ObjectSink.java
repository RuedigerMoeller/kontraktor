package org.nustaq.kontraktor.remoting.base;

/**
 * Created by ruedi on 09/05/15.
 */
public interface ObjectSink {

    public void receiveObject(Object received);
    public void sinkClosed();

}
