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

    public <K> AddMessage add(K key, Object ... keyVals) {
        Object record = record(key, keyVals);
        return new AddMessage((Record) record);
    }

    public <K> PutMessage put(K key, Object ... keyVals) {
        Object record = record(key, keyVals);
        return new PutMessage((Record)record);
    }

    public <K> AddMessage addOrUpdate(K key, Object ... keyVals) {
        Object record = record(key, keyVals);
        return new AddMessage(true,(Record) record);
    }

    public <K> UpdateMessage updateWithForced(K key, Set<String> forced, Object ... keyVals) {
        UpdateMessage update = update(key, keyVals);
        update.setForcedUpdateFields(forced);
        return update;
    }

    public <K> UpdateMessage update(K key, Object ... keyVals) {
        String fi[] = new String[keyVals.length/2];
        for (int i = 0; i < fi.length; i++) {
            fi[i] = (String) keyVals[i*2];
        }
        Diff d = new Diff(fi,null);
        Object record = record(key, keyVals);
        return new UpdateMessage(d, (Record) record, null);
    }

    public <K> Record record(K key, Object ... keyVals) {
        MapRecord res = MapRecord.New(key);
        return buildRecord(res, keyVals);
    }

    public <K> Record buildRecord(Record res, Object[] keyVals) {
        for (int i = 0; i < keyVals.length; i+=2) {
            Object k = keyVals[i];
            Object v = keyVals[i+1];
            res.put((String) k,v);
        }
        return res;
    }

    public <K> RemoveMessage remove(K key) {
        return new RemoveMessage(MapRecord.New(key));
    }

    public <K> ChangeMessage done() {
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
