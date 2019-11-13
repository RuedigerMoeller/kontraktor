package org.nustaq.reallive.query;

/**
 * Created by moelrue on 27.08.2015.
 */
public class LongValue implements Value, NumberValue {

    long value;
    QToken token;

    public LongValue(long value, QToken token) {
        this.value = value; this.token = token;
    }

    @Override
    public QToken getToken() {
        return token;
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
    public Object getValue() {
        return value;
    }

    @Override
    public Value negate() {
        return new LongValue(value*-1, token);
    }

    @Override
    public boolean isEmpty() {
        return value == 0;
    }

    @Override
    public String toString() {
        return "LongValue{" +
                "value=" + value +
                '}';
    }
}
