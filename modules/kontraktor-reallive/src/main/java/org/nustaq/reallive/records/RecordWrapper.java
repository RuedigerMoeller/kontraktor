package org.nustaq.reallive.records;

import org.nustaq.reallive.api.Record;

/**
 * Created by ruedi on 22/08/15.
 *
 * base for app specific typed wrappers. Wrappers submitted/stored to reallive will be automatically unwrapped
 */
public class RecordWrapper implements Record {

    protected Record record;

    public static <T> RecordWrapper Wrap(Record rec) {
        if ( rec instanceof RecordWrapper ) {
            return (RecordWrapper) rec;
        }
        return new RecordWrapper(rec);
    }

    protected RecordWrapper(Record record) {
        this.record = record;
        if ( record instanceof RecordWrapper && record instanceof PatchingRecord == false) {
            int debug = 1;
        }
    }

    public RecordWrapper(String key) {
        this.record = MapRecord.New(key);
    }

    public Record getRecord() {
        return record;
    }

    public String getKey() {
        return record.getKey();
    }

    @Override
    public long getLastModified() {
        return record.getLastModified();
    }

    @Override
    public void internal_setLastModified(long tim) {
        record.internal_setLastModified(tim);
    }

    @Override
    public void internal_incSequence() {
        record.internal_incSequence();
    }

    @Override
    public long getSequence() {
        return record.getSequence();
    }

    @Override
    public void internal_updateLastModified() {
        record.internal_updateLastModified();
    }

    @Override
    public void key(String key) {
        record.key(key);
    }

    @Override
    public String[] getFields() {
        return record.getFields();
    }

    @Override
    public Object get(String field) {
        return record.get(field);
    }

    @Override
    public Record put(String field, Object value) {
        return record.put(field,value);
    }

    @Override
    public String toString() {
        return "RecordWrapper{" +
                   "record=" + record +
                   '}';
    }

    @Override
    public int hashCode() {
        return record.getKey().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj instanceof Record ) {
            return ((Record) obj).getKey().equals(getKey());
        }
        return super.equals(obj);
    }
}
