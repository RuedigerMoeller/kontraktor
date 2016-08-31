package org.nustaq.reallive.impl;

import org.nustaq.reallive.interfaces.*;
import org.nustaq.reallive.messages.*;
import org.nustaq.reallive.records.*;

import java.util.*;

/**
 * Created by ruedi on 04.08.2015.
 */
public class RLUtil {

    static RLUtil instance = new RLUtil();

    public static RLUtil get() {
        return instance;
    }

    public <K> AddMessage<K> add( K key, Object ... keyVals) {
        Object record = record(key, keyVals);
        return new AddMessage<>((Record) record);
    }

    public <K> PutMessage<K> put( K key, Object ... keyVals) {
        Object record = record(key, keyVals);
        return new PutMessage<>((Record<K>)record);
    }

    public <K> AddMessage<K> addOrUpdate( K key, Object ... keyVals) {
        Object record = record(key, keyVals);
        return new AddMessage<>(true,(Record<K>) record);
    }

    public <K> UpdateMessage<K> updateWithForced(K key, Set<String> forced, Object ... keyVals) {
        UpdateMessage<K> update = update(key, keyVals);
        update.setForcedUpdateFields(forced);
        return update;
    }

    public <K> UpdateMessage<K> update( K key, Object ... keyVals) {
        String fi[] = new String[keyVals.length/2];
        for (int i = 0; i < fi.length; i++) {
            fi[i] = (String) keyVals[i*2];
        }
        Diff d = new Diff(fi,null);
        Object record = record(key, keyVals);
        return new UpdateMessage<>(d, (Record<K>) record, null);
    }

    public <K> Record<K> record( K key, Object ... keyVals) {
        MapRecord<K> res = MapRecord.New(key);
        return buildRecord(res, keyVals);
    }

    public <K> Record<K> buildRecord(Record<K> res, Object[] keyVals) {
        for (int i = 0; i < keyVals.length; i+=2) {
            Object k = keyVals[i];
            Object v = keyVals[i+1];
            res.put((String) k,v);
        }
        return res;
    }

    public <K> RemoveMessage<K> remove(K key) {
        return new RemoveMessage<>(MapRecord.New(key));
    }

    public <K> ChangeMessage<K> done() {
        return new QueryDoneMessage();
    }

    public boolean isEqual(Record rlRec, Record copy) {
        String[] fields = rlRec.getFields();
        if ( fields.length != copy.getFields().length )
            return false;
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            Object a = rlRec.get(field);
            Object b = copy.get(field);
            if ( ! Objects.deepEquals(a,b) ) {
                return false;
            }
        }
        return true;
    }
}
