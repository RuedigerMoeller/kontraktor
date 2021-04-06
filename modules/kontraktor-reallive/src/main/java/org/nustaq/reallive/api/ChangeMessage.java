package org.nustaq.reallive.api;

import java.io.Serializable;
import java.util.*;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface ChangeMessage extends Serializable {

    int ADD = 0;
    int REMOVE = 1;
    int UPDATE = 2;
    int QUERYDONE = 3;
    int PUT = 4;

    int getType();
    int getSenderId();
    ChangeMessage senderId(int id);

    String getKey();
    ChangeMessage reduced(String[] reducedFields);
    ChangeMessage omit(String fields[]);

    default Set<String> getForcedUpdateFields() {
        return null;
    }
    default boolean isDoneMsg() { return getType() == QUERYDONE; }
    default boolean isAdd() { return getType() == ADD; }
    default Record getRecord() { return null; }
}
