package org.nustaq.reallive;

import org.nustaq.serialization.FSTClazzInfo;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by ruedi on 05.07.14.
 */
public class ChangeBroadcast<T extends Record> implements Serializable {

    public static final int UPDATE    = 0;
    public static final int ADD       = 1;
    public static final int REMOVE    = 2;
    public static final int OPERATION = 3;
    public static final int SNAPSHOT_DONE = 4;
    public static final int ERROR = 5;

    public static <T extends Record> ChangeBroadcast<T> NewSnapFin(String tableId, int origin) {
        return new ChangeBroadcast(ChangeBroadcast.SNAPSHOT_DONE, tableId, null, null, null, origin);
    }

    public static <T extends Record> ChangeBroadcast NewUpdate(String tableId, T newRecord, RecordChange appliedChange) {
        return new ChangeBroadcast(UPDATE,tableId,newRecord.getRecordKey(),newRecord,appliedChange, appliedChange.getOriginator());
    }

    public static <T extends Record> ChangeBroadcast<T> NewAdd(String tableId, T record, int origin) {
        return new ChangeBroadcast<>(ADD,tableId,record.getRecordKey(),record,null,origin);
    }

    public static <T extends Record> ChangeBroadcast<T> NewUpdate(String tableId, T t, String[] fieldNames, Object[] oldValues, FSTClazzInfo inf, int origin) {
        ChangeBroadcast changeBroadcast = new ChangeBroadcast(UPDATE, tableId, t.getRecordKey(), t, null, origin);
        changeBroadcast.appliedChange = new RecordChange(t.getRecordKey());
        changeBroadcast.appliedChange.setChanges(fieldNames,oldValues,t,inf);
        return changeBroadcast;
    }

    public static <T extends Record> ChangeBroadcast<T> NewError(String tableId, Object e, int origin) {
        ChangeBroadcast<T> tChangeBroadcast = new ChangeBroadcast<>(ERROR, tableId, null, null, null, origin);
        tChangeBroadcast.setError(e);
        return tChangeBroadcast;
    }

    public static <T extends Record> ChangeBroadcast<T> NewRemove(String tableId, T record, int origin) {
        return new ChangeBroadcast<>(REMOVE,tableId,record.getRecordKey(),record,null, origin);
    }

    private ChangeBroadcast(int type, String tableId, String recordKey, T newRecord, RecordChange<String, T> appliedChange, int origin) {
        this.type = type;
        this.tableId = tableId;
        this.newRecord = newRecord;
        this.appliedChange = appliedChange;
        this.recordKey = recordKey;
        this.originator = origin;
    }

    int type;
    String tableId;
    String recordKey;
    int originator;

    T newRecord; // state of record after update
    RecordChange<String,T> appliedChange; // in case of update contains old values of updated fields
    Object error;

    public int getOriginator() {
        return originator;
    }

    public Object getError() {
        return error;
    }

    public void setError(Object error) {
        this.error = error;
    }

    public boolean isSnapshotDone() {
        return getType() == SNAPSHOT_DONE;
    }

    public String getRecordKey() {
        return recordKey;
    }

    public int getType() {
        return type;
    }

    public String getTableId() {
        return tableId;
    }

    public T getRecord() {
        return newRecord;
    }

    public RecordChange<String, T> getAppliedChange() {
        return appliedChange;
    }

    public void toOld() {
        appliedChange.setOld(newRecord);
    }

    public void toNew() {
        appliedChange.setNew(newRecord);
    }

    public String toString() {
        switch (type) {
            case ADD:
                return "ChangeBC ADD on " + recordKey + " " + newRecord;
            case REMOVE:
                return "ChangeBC REMOVE on " + recordKey + " " + newRecord;
            case UPDATE:
                return "ChangeBC UPDATE on " + recordKey + " " + Arrays.toString(newRecord.toFieldNames(appliedChange.getChangedFields()));
            case SNAPSHOT_DONE:
                return "ChangeBC SNAPSHOT_DONE on " + tableId;
            case ERROR:
                return "ChangeBC ERROR on " + tableId+" "+error;
            default:
                return super.toString();
        }
    }

    public boolean isError() {
        return getType() == ERROR;
    }

    public boolean isAdd() {
        return getType() == ADD;
    }

    public boolean isARU() {
        return type == ADD || type == UPDATE || type == REMOVE;
    }

}
