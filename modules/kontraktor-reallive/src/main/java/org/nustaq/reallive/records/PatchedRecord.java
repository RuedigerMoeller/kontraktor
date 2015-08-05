package org.nustaq.reallive.records;

import org.nustaq.reallive.api.*;

/**
 * Created by ruedi on 04/08/15.
 */
public class PatchedRecord implements Record {

    protected Record wrapped;

    public PatchedRecord(Record wrapped) {
        this.wrapped = wrapped;
    }

    public Record getWrapped() {
        return wrapped;
    }

    public void setWrapped(Record wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Object getKey() {
        return wrapped.getKey();
    }

    @Override
    public String[] getFields() {
        return wrapped.getFields();
    }

    @Override
    public Object get(String field) {
        return wrapped.get(field);
    }

    @Override
    public Record put(String field, Object value) {
        return wrapped.put(field,value);
    }
}
