package org.nustaq.reallive.messages;

import org.nustaq.reallive.interfaces.*;

/**
 * Created by moelrue on 03.08.2015.
 *
 * Processing if received as change request:
 * - if diff is != null => apply diff
 * - else take new Record and compare against old
 */
public class UpdateMessage<K> implements ChangeMessage<K> {

    final Diff diff;   // can be null => then just compare with current record
    final Record<K> newRecord; // can nevere be null
    final boolean addIfNotExists ;

    public UpdateMessage(Diff diff, Record<K> newRecord) {
        this.diff = diff;
        this.newRecord = newRecord;
        this.addIfNotExists = true;
    }

    public UpdateMessage(Diff diff, Record<K> newRecord, boolean addIfNotExists) {
        this.addIfNotExists = addIfNotExists;
        this.newRecord = newRecord;
        this.diff = diff;
    }

    @Override
    public int getType() {
        return UPDATE;
    }

    @Override
    public K getKey() {
        return newRecord.getKey();
    }

    @Override
    public ChangeMessage reduced(String[] reducedFields) {
        return new UpdateMessage<K>(
            diff.reduced(reducedFields),
            newRecord.reduced(reducedFields),
            addIfNotExists);
    }

    public Diff getDiff() {
        return diff;
    }

    public Record<K> getNewRecord() {
        return newRecord;
    }

    public boolean isAddIfNotExists() {
        return addIfNotExists;
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
