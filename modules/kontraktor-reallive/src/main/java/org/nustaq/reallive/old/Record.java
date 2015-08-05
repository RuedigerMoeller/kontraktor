package org.nustaq.reallive.old;

import org.nustaq.reallive.old.sys.annotations.ColOrder;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.util.FSTUtil;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by ruedi on 01.06.14.
 */
public class Record implements Serializable {

    @ColOrder(-2)
    String recordKey;
    @ColOrder(-1)
    int version;

    transient Record originalRecord;
    transient RecordContext table;

    public Record() {

    }

    public Record(Record originalRecord) {
        this.originalRecord = originalRecord;
        this.recordKey = originalRecord.getRecordKey();
        this.table = originalRecord.table;
    }

    public Record(String key) {
        this(key,null);
    }

    public Record(String key, RecordContext table) {
        this.recordKey = key;
        this.table = table;
    }

    public void _setTable(RecordContext table) {
        this.table = table;
    }

    public void _setRecordKey(String id) {
        this.recordKey = id;
    }

    public void _setOriginalRecord(Record org) {
        originalRecord = org;
    }

    public String getRecordKey()
    {
        return recordKey;
    }

    public <T extends Record> T createCopy() {
        T record = null;
        try {
            record = (T) getClass().newInstance();
        } catch (Exception e) {
            FSTUtil.rethrow(e);
        }
        copyTo(record);
        return record;
    }

    public void copyTo( Record other) {
        FSTClazzInfo.FSTFieldInfo[] fieldInfo = getFieldInfo();
        other.table = table;

        for (int i = 0; i < fieldInfo.length; i++) {
            FSTClazzInfo.FSTFieldInfo fi = fieldInfo[i];
            try {
                if ( fi.isPrimitive() ) {
                    switch (fi.getIntegralCode(fi.getType())) {
                        case FSTClazzInfo.FSTFieldInfo.BOOL:
                            fi.setBooleanValue(other, fi.getBooleanValue(this) );
                            break;
                        case FSTClazzInfo.FSTFieldInfo.BYTE:
                            fi.setByteValue(other, (byte) fi.getByteValue(this));
                            break;
                        case FSTClazzInfo.FSTFieldInfo.CHAR:
                            fi.setCharValue(other, (char) fi.getCharValue(this));
                            break;
                        case FSTClazzInfo.FSTFieldInfo.SHORT:
                            fi.setShortValue(other, (short) fi.getShortValue(this));
                            break;
                        case FSTClazzInfo.FSTFieldInfo.INT:
                            fi.setIntValue(other, fi.getIntValue(this) );
                            break;
                        case FSTClazzInfo.FSTFieldInfo.LONG:
                            fi.setLongValue(other, fi.getLongValue(this) );
                            break;
                        case FSTClazzInfo.FSTFieldInfo.FLOAT:
                            fi.setFloatValue(other, fi.getFloatValue(this) );
                            break;
                        case FSTClazzInfo.FSTFieldInfo.DOUBLE:
                            fi.setDoubleValue(other, fi.getDoubleValue(this) );
                            break;
                    }
                } else {
                    fi.setObjectValue(other, fi.getObjectValue(this));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public ChangeBroadcast computeAddBcast(String tableId, int originator) {
        return ChangeBroadcast.NewAdd(tableId, this, originator);
    }

    public ChangeBroadcast computeUpdateBcast(boolean addIfNotPresent, int originator) {
        if (originalRecord == null)
            throw new RuntimeException("original record must not be null for update");
        if (recordKey == null)
            throw new RuntimeException("recordKey must not be null on update");
        RecordChange recordChange = computeDiff(true);
        recordChange.setOriginator(originator);
        copyTo(originalRecord); // nil all diffs. Once prepared, record can be reused for updateing
        ChangeBroadcast changeBroadcast = ChangeBroadcast.NewUpdate(table.getTableId(), this, recordChange);
        changeBroadcast.setAddIfNotPresent(addIfNotPresent);
        return changeBroadcast;
    }

    public RecordChange computeDiff(String... forced) {
        return computeDiff(false, forced);
    }

    public RecordChange computeDiff(boolean withOld, String ... forced) {
        return computeDiff(originalRecord,withOld, forced);
    }

    /**
     * @return a record change containing newFieldValues and changed fields
     */
    public RecordChange computeDiff(Record oldRecord, boolean withOld, String ... forced) {
        FSTClazzInfo.FSTFieldInfo[] fieldInfo = getFieldInfo();

        RecordChange change = new RecordChange(getRecordKey());

        ArrayList<FSTClazzInfo.FSTFieldInfo> changedFields = new ArrayList<>();
        ArrayList changedValues = new ArrayList();

        for (int i = 0; i < fieldInfo.length; i++) {
            FSTClazzInfo.FSTFieldInfo fi = fieldInfo[i];
            boolean changed = false;
            try {
                if ( fi.isPrimitive() ) {
                    switch (fi.getIntegralCode(fi.getType())) {
                        case FSTClazzInfo.FSTFieldInfo.BOOL:
                            changed = fi.getBooleanValue(oldRecord) != fi.getBooleanValue(this);
                            break;
                        case FSTClazzInfo.FSTFieldInfo.BYTE:
                            changed = fi.getByteValue(oldRecord) != fi.getByteValue(this);
                            break;
                        case FSTClazzInfo.FSTFieldInfo.CHAR:
                            changed = fi.getCharValue(oldRecord) != fi.getCharValue(this);
                            break;
                        case FSTClazzInfo.FSTFieldInfo.SHORT:
                            changed = fi.getShortValue(oldRecord) != fi.getShortValue(this);
                            break;
                        case FSTClazzInfo.FSTFieldInfo.INT:
                            changed = fi.getIntValue(oldRecord) != fi.getIntValue(this);
                            break;
                        case FSTClazzInfo.FSTFieldInfo.LONG:
                            changed = fi.getLongValue(oldRecord) != fi.getLongValue(this);
                            break;
                        case FSTClazzInfo.FSTFieldInfo.FLOAT:
                            changed = fi.getFloatValue(oldRecord) != fi.getFloatValue(this);
                            break;
                        case FSTClazzInfo.FSTFieldInfo.DOUBLE:
                            changed = fi.getDoubleValue(oldRecord) != fi.getDoubleValue(this);
                            break;
                    }
                } else {
                    changed = fi.getObjectValue(oldRecord) != fi.getObjectValue(this);
                }
                if ( forced != null && ! changed ) {
                    for (int j = 0; j < forced.length; j++) {
                        if (fi.getField().getName().equals(forced[j])) {
                            changed = true;
                            break;
                        }
                    }
                }
                if ( changed ) {
                    changedFields.add(fi);
                    changedValues.add(fi.getField().get(this));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        change.setChanges(changedFields, changedValues, withOld ? oldRecord : null);
        return change;
    }

    protected FSTClazzInfo.FSTFieldInfo[] getFieldInfo() {
        return table.getFieldInfo();
    }

    public String toString() {
        String res = "["+getClass().getSimpleName()+" ";
        FSTClazzInfo.FSTFieldInfo[] fieldInfo = getFieldInfo();
        for (int i = 0; i < fieldInfo.length; i++) {
            FSTClazzInfo.FSTFieldInfo fstFieldInfo = fieldInfo[i];
            try {
                res += fstFieldInfo.getField().getName() + ": " + fstFieldInfo.getField().get(this) + ", ";
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return res + " ]";
    }

    public int getFieldId(String fieldName) {
        return table.getFieldInfo(fieldName).getIndexId();
    }

    public String[] toFieldNames(int fieldIndex[]) {
        String res[] = new String[fieldIndex.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = getFieldInfo()[fieldIndex[i]].getField().getName();
        }
        return res;
    }

    public Object getField( int indexId ) {
        FSTClazzInfo.FSTFieldInfo fi = getFieldInfo()[indexId];
        try {
            return fi.getField().get(this);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setField( int indexId, Object value ) {
        FSTClazzInfo.FSTFieldInfo fi = getFieldInfo()[indexId];
        try {
            fi.getField().set(this, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public int getVersion() {
        return version;
    }
    public void incVersion() {
        version++;
    }

    public void prepareForUpdate(boolean addIfNotPresent) {
        try {
            Record record = getClass().newInstance();
            copyTo(record);
            _setOriginalRecord(record);
        } catch (Exception e) {
            FSTUtil.rethrow(e);
        }
    }
}
