package org.nustaq.reallive.records;
import org.nustaq.reallive.impl.RLUtil;
import org.nustaq.reallive.interfaces.Record;
import org.nustaq.reallive.messages.UpdateMessage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by ruedi on 10.01.16.
 *
 * overrides on write
 */
public class PatchingRecord extends RecordWrapper {
    MapRecord override;

    public PatchingRecord(Record record) {
        super(record);
        if ( record instanceof RecordWrapper ) {
            int debug = 1;
        }
    }

    public boolean isModified() {
        return override != null;
    }

    @Override
    public String[] getFields() {
        if ( override != null ) {
            HashSet hs = new HashSet();
            hs.addAll(Arrays.asList(override.getFields()));
            hs.addAll(Arrays.asList(super.getFields()));
            String r[] = new String[hs.size()];
            hs.toArray(r);
            return r;
        }
        return super.getFields();
    }

    @Override
    public Object get(String field) {
        if (override!= null) {
            final Object r = override.get(field);
            if ( r != null ) {
                return r;
            }
        }
        return super.get(field);
    }

    @Override
    public Record put(String field, Object value) {
        if ( override == null )
            override = MapRecord.New(getKey());
        return override.put(field, value);
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

    public <K> void reset(Record<K> input) {
        record = input;
        override = null;
    }

    public <K> UpdateMessage<K> getUpdates() {
        if ( override == null )
            return null;
        Object update[] = new Object[override.size()*2];
        int idx = 0;
        for (Iterator iterator = override.map.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry next = (Map.Entry) iterator.next();
            update[idx++] = next.getKey();
            update[idx++] = next.getValue();
        }
        return RLUtil.get().update((K) getKey(),update);
    }

    public <K> Record<K> unwrapOrCopy() {
        if ( override == null )
            return record;
        MapRecord res = MapRecord.New(getKey());
        final String[] fields = getFields();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            res.put(field,get(field));
        }
        return res;
    }
}
