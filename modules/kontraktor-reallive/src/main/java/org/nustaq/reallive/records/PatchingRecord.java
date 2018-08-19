package org.nustaq.reallive.records;

import org.nustaq.reallive.impl.RLUtil;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.messages.UpdateMessage;

import java.util.*;

/**
 * Created by ruedi on 10.01.16.
 * <p>
 * overrides on write. Used to enable queries to patch result records (e.g. in order
 * to submit results of cpu intensive computations)
 */
public class PatchingRecord extends RecordWrapper {

    public static final Object NULL = new Object();

    MapRecord override;
    HashSet<String> forcedUpdate;

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

    @Override
    public Object get(String field) {
        if (override != null) {
            final Object r = override.get(field);
            if (r != null) {
                return r == NULL ? null : r;
            }
        }
        return super.get(field);
    }

    @Override
    public Record put(String field, Object value) {
        if (override == null)
            override = MapRecord.New(getKey());
        if ( value == null )
            value = NULL;
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

    public void reset(Record input) {
        record = input;
        override = null;
        forcedUpdate = null;
    }

    public void forceUpdate(String name) {
        if (forcedUpdate == null) {
            forcedUpdate = new HashSet();
        }
        forcedUpdate.add(name);
    }

    public Set<String> getForcedUpdates() {
        return forcedUpdate;
    }

    public UpdateMessage getUpdates() {
        if (override == null)
            return null;
        Object update[] = new Object[override.size() * 2];
        int idx = 0;
        for (Iterator iterator = override.map.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry next = (Map.Entry) iterator.next();
            update[idx++] = next.getKey();
            update[idx++] = next.getValue();
        }
        return RLUtil.get().updateWithForced(getKey(), forcedUpdate, update);
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
}
