package org.nustaq.reallive;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.reallive.impl.RLTable;
import org.nustaq.reallive.sys.annotations.ColOrder;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.util.FSTUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.function.Predicate;

/**
 * Created by ruedi on 01.06.14.
 */
public class Record implements Serializable {

    public static enum Mode {
        ADD,
        UPDATE,
        NONE, UPDATE_OR_ADD,
    }
    transient Mode mode = Mode.NONE;
    transient FSTClazzInfo clazzInfo;

    @ColOrder(-2)
    String recordKey;
    @ColOrder(-1)
    int version;

    transient Record originalRecord;
    transient RLTable table;

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

    public Record(String key, RLTable schema) {
        this.recordKey = key;
        this.table = schema;
    }

    public void _setTable(RLTable table) {
        this.table = table;
    }

    public void _setRecordKey(String id) {
        this.recordKey = id;
    }

    public void _setOriginalRecord(Record org) {
        originalRecord = org;
    }

    public void _setMode(Mode newMode) {
        mode = newMode;
    }

    public Mode getMode() {
        return mode;
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
        FSTClazzInfo classInfo = getClassInfo();
        if ( other.getClass() != getClass() )
            throw new RuntimeException("other record must be of same type");
        FSTClazzInfo.FSTFieldInfo[] fieldInfo = classInfo.getFieldInfo();
        other.table = table;
        other.clazzInfo = clazzInfo;

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

    public IPromise<Boolean> applyCAS(Predicate condition, int mutatorId) {
        if ( mode == Mode.UPDATE ) {
            if ( originalRecord == null )
                throw new RuntimeException("original record must not be null for update");
            if ( recordKey == null )
                throw new RuntimeException("recordKey must not be null on update");
            RecordChange recordChange = computeDiff();
            recordChange.setOriginator(mutatorId);
            boolean result = table.updateCAS(recordChange, condition);
            copyTo(originalRecord); // nil all diffs. Once prepared, record can be reused for updateing
            return new Promise<>(result);
        } else
            throw new RuntimeException("wrong mode. applyCAS only possible for update of existing records");
    }

    /**
     * Important: this method works only if the record was prepared/created by the
     * createXX/prepareXX methods of a RLTable.
     *
     * persist an add or update of a record. The id given is added to a resulting change broadcast, so a
     * process is capable to identify changes caused by itself.
     */
    public IPromise<String> applyForced(int mutatorId, String ... fieldNames) {
        if ( table == null ) {
            throw new RuntimeException("no table reference. use createForXX/prepareXX methods at RLTable to get valid instances.");
        }
        if ( mode == Mode.ADD ) {
            return new Promise<>(table.addGetId(this, mutatorId));
        } else
        if ( mode == Mode.UPDATE || mode == Mode.UPDATE_OR_ADD ) {
            if ( originalRecord == null )
                throw new RuntimeException("original record must not be null for update");
            if ( recordKey == null )
                throw new RuntimeException("recordKey must not be null on update");
            RecordChange recordChange = computeDiff( fieldNames );
            if ( recordChange.getChangedFields().length > 0 || mode == Mode.UPDATE_OR_ADD ) {
                recordChange.setOriginator(mutatorId);
                table.update(recordChange, mode == Mode.UPDATE_OR_ADD);
                copyTo(originalRecord); // nil all diffs. Once prepared, record can be reused for updateing
            }
            return new Promise<>(recordKey);
        } else
            throw new RuntimeException("wrong mode. Use table.create* and table.prepare* methods.");
    }

    /**
     * Important: this method works only if the record was prepared/created by the
     * createXX/prepareXX methods of a RLTable.
     *
     * persist an add or update of a record. The id given is added to a resulting change broadcast, so a
     * process is capable to identify changes caused by itself.
     */
    public IPromise<String> apply(int mutatorId) {
        return applyForced(mutatorId);
    }

    public ChangeBroadcast computeBcast(String tableId, int originator) {
        if ( originalRecord == null )
            throw new RuntimeException("record not prepared");
        if ( mode == Mode.ADD ) {
            return ChangeBroadcast.NewAdd(tableId, this, originator);
        }
        if ( mode == Mode.UPDATE || mode == Mode.UPDATE_OR_ADD ) {
            if ( originalRecord == null )
                throw new RuntimeException("original record must not be null for update");
            if ( recordKey == null )
                throw new RuntimeException("recordKey must not be null on update");
            RecordChange recordChange = computeDiff(true);
            recordChange.setOriginator(originator);
            copyTo(originalRecord); // nil all diffs. Once prepared, record can be reused for updateing
            return ChangeBroadcast.NewUpdate(tableId, this, recordChange);
        } else
            throw new RuntimeException("wrong mode");
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
        FSTClazzInfo classInfo = getClassInfo();
        FSTClazzInfo.FSTFieldInfo[] fieldInfo = classInfo.getFieldInfo();

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

    public FSTClazzInfo getClassInfo() {
        if ( getRealLive() == null )
            return clazzInfo;
        return getRealLive().getConf().getClassInfo(getClass());
    }

    public void setClazzInfo(FSTClazzInfo clazzInfo) {
        this.clazzInfo = clazzInfo;
    }

    public RealLive getRealLive() {
        if ( table == null )
            return null;
        return table.getRealLive();
    }

    public String toString() {
        String res = "["+getClass().getSimpleName()+" ";
        if ( getClassInfo() == null ) {
            return "[Record key:"+getRecordKey()+"]";
        } else {
            FSTClazzInfo.FSTFieldInfo[] fieldInfo = getClassInfo().getFieldInfo();
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
    }

    public int getFieldId(String fieldName) {
        return getClassInfo().getFieldInfo(fieldName,null).getIndexId();
    }

    public String[] toFieldNames(int fieldIndex[]) {
        String res[] = new String[fieldIndex.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = getClassInfo().getFieldInfo()[fieldIndex[i]].getField().getName();
        }
        return res;
    }

    public Object getField( int indexId ) {
        FSTClazzInfo.FSTFieldInfo fi = getClassInfo().getFieldInfo()[indexId];
        try {
            return fi.getField().get(this);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setField( int indexId, Object value ) {
        FSTClazzInfo.FSTFieldInfo fi = getClassInfo().getFieldInfo()[indexId];
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
        prepareForUpdate(addIfNotPresent,null);
    }

    public void prepareForUpdate(boolean addIfNotPresent, FSTClazzInfo info) {
        if (info != null) {
            clazzInfo = info;
        }
        try {
            Record record = getClass().newInstance();
            _setMode(addIfNotPresent ? Mode.UPDATE_OR_ADD : Mode.UPDATE);
            copyTo(record);
            _setOriginalRecord(record);
        } catch (Exception e) {
            FSTUtil.rethrow(e);
        }
    }
}
