package org.nustaq.reallive.messages;

import org.nustaq.reallive.api.*;

/**
 * Created by moelrue on 06.08.2015.
 */
public class ControlMessage implements ChangeMessage {

    @Override
    public int getType() {
        return QUERYDONE;
    }

    @Override
    public Object getKey() {
        return null;
    }

}
