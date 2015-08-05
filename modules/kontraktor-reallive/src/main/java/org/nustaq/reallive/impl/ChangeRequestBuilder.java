package org.nustaq.reallive.impl;

import org.nustaq.reallive.api.*;
import org.nustaq.reallive.messages.*;
import org.nustaq.reallive.records.*;

/**
 * Created by ruedi on 04.08.2015.
 */
public class ChangeRequestBuilder {

    static ChangeRequestBuilder instance = new ChangeRequestBuilder();

    public static ChangeRequestBuilder get() {
        return instance;
    }

    public <K,V extends Record<K>> AddMessage<K,V> add( K key, Object ... keyVals) {
        Object record = record(key, keyVals);
        return new AddMessage<>((V) record);
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
}
