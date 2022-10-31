package org.nustaq.reallive.messages;

import org.nustaq.reallive.api.Record;

/**
 * used for administrative record management (e.g. move record to another node)
 * does not generate a change broadcast, does not update "lastModified" timestamp
 */
public class IdenticalPutMessage extends PutMessage {

    public IdenticalPutMessage(int senderId, Record record) {
        super(senderId, record);
    }

    @Override
    public boolean updateLastModified() {
        return false;
    }

    @Override
    public boolean generateChangeBroadcast() {
        return false;
    }
}
