package org.nustaq.reallive.query;

/**
 * Created by moelrue on 27.08.2015.
 */
public class StringValue implements Value {

    QToken token;
    String value;

    public StringValue(String value, QToken token) {
        this.value = value;
        this.token = token;
    }

    @Override
    public QToken getToken() {
        return token;
    }

    @Override
    public double getDoubleValue() {
        return Double.parseDouble(value);
    }

    @Override
    public long getLongValue() {
        return Long.parseLong(value);
    }

    @Override
    public String getStringValue() {
        return value;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public Value negate() {
        return isTrue() ? new StringValue("", token) : new StringValue("1",token);
    }

    @Override
    public boolean isEmpty() {
        return value == null || value.length() == 0;
    }

    @Override
    public String toString() {
        return "StringValue{" +
                "value='" + value + '\'' +
                '}';
    }
}
