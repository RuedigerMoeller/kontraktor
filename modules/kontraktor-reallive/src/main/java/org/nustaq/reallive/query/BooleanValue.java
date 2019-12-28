package org.nustaq.reallive.query;


/**
 * Created by moelrue on 27.08.2015.
 */
public class BooleanValue implements Value, NumberValue {

    boolean value;
    QToken token;

    public BooleanValue(boolean value, QToken token) {
        this.value = value; this.token = token;
    }

    @Override
    public QToken getToken() {
        return token;
    }

    @Override
    public double getDoubleValue() {
        return value ? 1 : 0;
    }

    @Override
    public long getLongValue() {
        return value ? 1 : 0;
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
        return new BooleanValue(!value, token);
    }

    @Override
    public boolean isEmpty() {
        return !value;
    }

    @Override
    public String toString() {
        return "BooleanValue{" +
            "value=" + value +
            '}';
    }
}

