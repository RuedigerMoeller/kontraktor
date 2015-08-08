package org.nustaq.reallive.impl;

import org.nustaq.reallive.interfaces.*;
import org.nustaq.reallive.messages.*;
import org.nustaq.reallive.records.*;

import java.util.Objects;

/**
 * Created by ruedi on 04.08.2015.
 */
public class RLUtil {

    static RLUtil instance = new RLUtil();

    public static RLUtil get() {
        return instance;
    }

    public <K,V extends Record<K>> AddMessage<K,V> add( K key, Object ... keyVals) {
        Object record = record(key, keyVals);
        return new AddMessage<>((V) record);
    }

    public <K,V extends Record<K>> PutMessage<K,V> put( K key, Object ... keyVals) {
        Object record = record(key, keyVals);
        return new PutMessage<>((V)record);
    }

    public <K,V extends Record<K>> AddMessage<K,V> addOrUpdate( K key, Object ... keyVals) {
        Object record = record(key, keyVals);
        return new AddMessage<>(true,(V) record);
    }

    public <K,V extends Record<K>> UpdateMessage<K,V> update( K key, Object ... keyVals) {
        String fi[] = new String[keyVals.length/2];
        for (int i = 0; i < fi.length; i++) {
            fi[i] = (String) keyVals[i*2];
        }
        Diff d = new Diff(fi,null);
        Object record = record(key, keyVals);
        return new UpdateMessage<>(d, (V) record);
    }

    public <K> Record<K> record( K key, Object ... keyVals) {
        MapRecord<K> res = new MapRecord<>(key);
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
        return new RemoveMessage<>(new MapRecord<>(key));
    }

    public <K, V extends Record<K>> ChangeMessage<K, V> done() {
        return new ControlMessage();
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
