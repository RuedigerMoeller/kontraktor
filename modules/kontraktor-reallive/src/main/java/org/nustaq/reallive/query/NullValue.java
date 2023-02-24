package org.nustaq.reallive.query;

public class NullValue implements Value {

    public final static NullValue NULL = new NullValue();

    @Override
    public QToken getToken() {
        return null;
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
        return "null";
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public Value negate() {
        return this;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }
}
