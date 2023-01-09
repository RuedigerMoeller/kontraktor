package org.nustaq.reallive.records;

import org.nustaq.reallive.api.Record;

import java.util.*;

/**
 * Created by ruedi on 10.01.16.
 * <p>
 * overrides on write. Used in filter to patch old/new record from a diff.
 * Only detects changes on top-level (not inside nested records)
 */
public class PatchingRecord extends RecordWrapper {

    MapRecord override;

    public PatchingRecord(Record record) {
        super(record);
        if (record instanceof RecordWrapper) {
            int debug = 1;
        }
    }

    public boolean isModified() {
        return override != null;
    }

    @Override
    public String[] getFields() {
        if (override != null) {
            HashSet hs = new HashSet();
            hs.addAll(Arrays.asList(override.getFields()));
            hs.addAll(Arrays.asList(super.getFields()));
            String r[] = new String[hs.size()];
            hs.toArray(r);
            return r;
        }
        return super.getFields();
    }

    public PatchingRecord shallowCopy() {
        PatchingRecord newReq = new PatchingRecord(record);
        if ( override != null )
            newReq.override = override.shallowCopy();
        return newReq;
    }

    @Override
    public Object get(String field) {
        if (override != null) {
            final Object r = override.get(field);
            if (r != null) {
                return r == _NULL_ ? null : r;
            }
        }
        return super.get(field);
    }

    @Override
    public Record internal_put(String field, Object value) {
        if (override == null)
            override = MapRecord.New(getKey());
        override.internal_put(field, value);
        return this;
    }

    @Override
    public Record put(String field, Object value) {
        if (override == null)
            override = MapRecord.New(getKey());
        if ( value == null )
            value = _NULL_;
        override.internal_put(field, value);
        return this;
    }

    @Override
    public String toString() {
        return "PatchingRecord{" +
            "override=" + override +
            "super=" + super.toString() +
            '}';
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    public void reset(Record input) {
        record = input;
        override = null;
    }

    public Record unwrapOrCopy() {
        if (override == null)
            return record;
        MapRecord res = MapRecord.New(getKey());
        final String[] fields = getFields();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            res.put(field, get(field));
        }
        res.lastModified = getLastModified();
        res.seq = getSequence();
        return res;
    }

    @Override
    public boolean containsKey(String x) {
        return super.containsKey(x) || override.containsKey(x);
    }
}
