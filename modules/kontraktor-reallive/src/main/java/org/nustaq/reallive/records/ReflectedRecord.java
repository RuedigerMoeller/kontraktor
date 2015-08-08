package org.nustaq.reallive.records;

import org.nustaq.reallive.interfaces.Record;
import org.nustaq.serialization.FSTConfiguration;

/**
 * Created by ruedi on 08.08.2015.
 */
public interface ReflectedRecord<K> extends Record<K> {
    @Override
    default String[] getFields() {
        return ReflectionHelper.getFields(getClass());
    }

    @Override
    default Object get(String field) {
        return ReflectionHelper.get(getClass(),field);
    }

    @Override
    default Record put(String field, Object value) {
        ReflectionHelper.put(getClass(),field,value);
        return this;
    }
}
