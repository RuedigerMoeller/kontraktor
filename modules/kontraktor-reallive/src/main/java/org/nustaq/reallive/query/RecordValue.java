package org.nustaq.reallive.query;

import org.nustaq.reallive.api.Record;

public class RecordValue implements Value {

    Record record;

    public RecordValue(Record record) {
        this.record = record;
    }

    @Override
    public QToken getToken() {
        return null;
    }

    public Record getRecord() {
        return record;
    }

    @Override
    public double getDoubleValue() {
        return 0;
    }

    @Override
    public long getLongValue() {
        return 0;
    }

    @Override
    public String getStringValue() {
        return null;
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public Value negate() {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
