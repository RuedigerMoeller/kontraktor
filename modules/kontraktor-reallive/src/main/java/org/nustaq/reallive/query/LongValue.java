package org.nustaq.reallive.query;

/**
 * Created by moelrue on 27.08.2015.
 */
public class LongValue implements Value {

    long value;

    public LongValue(long value) {
        this.value = value;
    }

    @Override
    public double getDoubleValue() {
        return value;
    }

    @Override
    public long getLongValue() {
        return value;
    }

    @Override
    public String getStringValue() {
        return String.valueOf(value);
    }

    @Override
    public Value negate() {
        return new LongValue(value*-1);
    }

    @Override
    public String toString() {
        return "LongValue{" +
                "value=" + value +
                '}';
    }
}
