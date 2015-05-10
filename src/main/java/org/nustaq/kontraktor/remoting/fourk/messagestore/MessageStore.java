package org.nustaq.kontraktor.remoting.fourk.messagestore;

import org.nustaq.offheap.bytez.ByteSource;

/**
 * Created by ruedi on 13/04/15.
 */
public interface MessageStore {

    public ByteSource getMessage( CharSequence queueId, long sequence );
    public void       putMessage( CharSequence queueId, long sequence, ByteSource message );
    public void       confirmMessage( CharSequence queueId, long sequence );

    public void killQueue( CharSequence queueId);

}
