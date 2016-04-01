package org.nustaq.reallive.records;

import org.nustaq.reallive.interfaces.Record;
import org.nustaq.reallive.records.MapRecord;

/**
 * Created by ruedi on 22/08/15.
 */
public class RecordWrapper<K> implements Record<K> {

    protected Record<K> record;

    public static <T> RecordWrapper<T> Wrap(Record<T> rec) {
        if ( rec instanceof RecordWrapper ) {
            return (RecordWrapper<T>) rec;
        }
        return new RecordWrapper<T>(rec);
    }

    protected RecordWrapper(Record<K> record) {
        this.record = record;
        if ( record instanceof RecordWrapper && record instanceof PatchingRecord == false) {
            int debug = 1;
        }
    }

    public RecordWrapper(K key) {
        this.record = MapRecord.New(key);
    }

    public Record<K> getRecord() {
        return record;
    }

    public K getKey() {
        return record.getKey();
    }

    @Override
    public void key(K key) {
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
