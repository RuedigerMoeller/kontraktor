package org.nustaq.reallive.messages;

import org.nustaq.reallive.api.*;

import java.util.*;

/**
 * Created by moelrue on 03.08.2015.
 *
 * Processing if received as change request:
 * - if diff is != null => apply diff
 * - else take new Record and compare against old
 */
public class UpdateMessage implements ChangeMessage {

    final Diff diff;   // can be null => then just compare with current record
    final Record newRecord; // can nevere be null
    final boolean addIfNotExists ;
    Set<String> forcedUpdateFields;
    int senderId;

    public UpdateMessage(int senderId, Diff diff, Record newRecord, Set<String> forcedUpdateFields) {
        this.diff = diff;
        this.newRecord = newRecord;
        this.addIfNotExists = true;
        this.forcedUpdateFields = forcedUpdateFields;
        this.senderId = senderId;
    }

    public UpdateMessage(int senderId,Diff diff, Record newRecord, Set<String> forcedUpdateFields, boolean addIfNotExists) {
        this.addIfNotExists = addIfNotExists;
        this.newRecord = newRecord;
        this.diff = diff;
        this.forcedUpdateFields = forcedUpdateFields;
        this.senderId = senderId;
    }

    public Set<String> getForcedUpdateFields() {
        return forcedUpdateFields;
    }

    public void setForcedUpdateFields(Set<String> forcedUpdateFields) {
        this.forcedUpdateFields = forcedUpdateFields;
    }

    @Override
    public int getType() {
        return UPDATE;
    }

    @Override
    public int getSenderId() {
        return senderId;
    }

    @Override
    public String getKey() {
        return newRecord.getKey();
    }

    @Override
    public ChangeMessage reduced(String[] reducedFields) {
        return new UpdateMessage(
            senderId, diff.reduced(reducedFields),
            newRecord.reduced(reducedFields),
            forcedUpdateFields,
            addIfNotExists);
    }

    public Diff getDiff() {
        return diff;
    }

    public Record getNewRecord() {
        return newRecord;
    }

    public boolean isAddIfNotExists() {
        return addIfNotExists;
    }

    public Record getOldRecord() {
        if ( diff.getChangedFields() != null && diff.getChangedFields().length > 0 ) {
            Record copied = getNewRecord().copied();
            for (int i = 0; i < diff.getChangedFields().length; i++) {
                String k = diff.getChangedFields()[i];
                copied.put(k,diff.getOldValues()[i]);
            }
            return copied;
        }
        return getRecord().copied();
    }

    @Override
    public Record getRecord() {
        return getNewRecord();
    }

    @Override
    public String toString() {
        return "UpdateMessage{" +
                "diff=" + diff +
                ", newRecord=" + newRecord.asString() +
                ", addIfNotExists=" + addIfNotExists +
                '}';
    }
}
