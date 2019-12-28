package org.nustaq.reallive.query;

/**
 * Created by ruedi on 27.08.2015.
 */
public class DoubleValue implements Value, NumberValue {

    double value;
    QToken  token;

    @Override
    public QToken getToken() {
        return token;
    }

    public DoubleValue(double value, QToken token) {
        this.value = value; this.token = token;
    }

    @Override
    public double getDoubleValue() {
        return value;
    }

    @Override
    public long getLongValue() {
        return (long) value;
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
        return new DoubleValue(value*-1, token);
    }

    @Override
    public boolean isEmpty() {
        return value == 0;
    }

    @Override
    public String toString() {
        return "DoubleValue{" +
                "value=" + value +
                '}';
    }

}
