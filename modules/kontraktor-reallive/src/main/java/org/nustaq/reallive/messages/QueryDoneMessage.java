package org.nustaq.reallive.messages;

import org.nustaq.reallive.api.*;

/**
 * Created by moelrue on 06.08.2015.
 */
public class QueryDoneMessage implements ChangeMessage {

    public QueryDoneMessage senderId(int id) {
        return this;
    }

    @Override
    public int getType() {
        return QUERYDONE;
    }

    @Override
    public int getSenderId() {
        return 0;
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public ChangeMessage reduced(String[] reducedFields) {
        return this;
    }

    @Override
    public String toString() {
        return "QueryDoneMessage{"+getType()+"}";
    }
}
