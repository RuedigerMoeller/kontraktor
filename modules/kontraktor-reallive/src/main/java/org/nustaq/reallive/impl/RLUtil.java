package org.nustaq.reallive.impl;

import org.nustaq.reallive.api.*;
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

    public static boolean isAllowedClazz(Class<?> clazz) {
        String name = clazz.getName();
        return isValidClazzName(name) ||
            (clazz.isArray() &&
                (clazz.getComponentType().isPrimitive() ||
                 isValidClazzName(clazz.getComponentType().getName())) || isValidClazz(clazz) );
    }

    private static boolean isValidClazz(Class clazz) {
        return (MapRecord.class.isAssignableFrom(clazz));
    }

    private static boolean isValidClazzName(String name) {
        return name.startsWith("java.") || name.startsWith("javax.");
    }

    public AddMessage add(int senderId, String key, Object... keyVals) {
        Object record = record(key, keyVals);
        return new AddMessage(senderId,(Record) record);
    }

    public PutMessage put(int senderId, String key, Object... keyVals) {
        Object record = record(key, keyVals);
        return new PutMessage(senderId, (Record) record);
    }

    public AddMessage addOrUpdate(int senderId, String key, Object... keyVals) {
        Object record = record(key, keyVals);
        return new AddMessage( senderId, true, (Record) record);
    }

    public UpdateMessage updateWithForced(int senderId, String key, Set forced, Object... keyVals) {
        UpdateMessage update = update(senderId,key, keyVals);
        update.setForcedUpdateFields(forced);
        return update;
    }

    public UpdateMessage update(int senderId, String key, Object... keyVals) {
        String fi[] = new String[keyVals.length / 2];
        for (int i = 0; i < fi.length; i++) {
            fi[i] = (String) keyVals[i * 2];
        }
        Diff d = new Diff(fi, null);
        Object record = record(key, keyVals);
        return new UpdateMessage(senderId, d, (Record) record, null);
    }

    public Record record(String key, Object... keyVals) {
        MapRecord res = MapRecord.New(key);
        return buildRecord(res, keyVals);
    }

    public Record buildRecord(Record res, Object[] keyVals) {
        if ( keyVals.length % 2 != 0) {
            if ( keyVals.length == 1 && keyVals[0] instanceof Record)
                throw new RuntimeException("invalid length of keyval array, try 'setRecord' method ");
            throw new RuntimeException("invalid length of keyval array");
        }
        for (int i = 0; i < keyVals.length; i += 2) {
            Object k = keyVals[i];
            Object v = keyVals[i + 1];
            res.put((String) k, v);
        }
        return res;
    }

    public RemoveMessage remove(int senderId, String key) {
        return new RemoveMessage(senderId,MapRecord.New(key));
    }

    public ChangeMessage done() {
        return new QueryDoneMessage();
    }

    public boolean isEqual(Record rlRec, Record copy) {
        String[] fields = rlRec.getFields();
        if (fields.length != copy.getFields().length)
            return false;
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            Object a = rlRec.get(field);
            Object b = copy.get(field);
            if (!Objects.deepEquals(a, b)) {
                return false;
            }
        }
        return true;
    }
}
