package org.nustaq.reallive.messages;

import org.nustaq.reallive.interfaces.*;

/**
 * Created by moelrue on 06.08.2015.
 */
public class QueryDoneMessage implements ChangeMessage {

    @Override
    public int getType() {
        return QUERYDONE;
    }

    @Override
    public Object getKey() {
        return null;
    }

    @Override
    public String toString() {
        return "ControlMessage{"+getType()+"}";
    }
}
