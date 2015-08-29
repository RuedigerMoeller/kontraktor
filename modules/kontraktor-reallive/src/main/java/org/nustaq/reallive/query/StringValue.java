package org.nustaq.reallive.query;

/**
 * Created by moelrue on 27.08.2015.
 */
public class StringValue implements Value {

    String value;

    public StringValue(String value) {
        this.value = value;
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
    public Value negate() {
        return isTrue() ? new StringValue("") : new StringValue("1");
    }

    @Override
    public String toString() {
        return "StringValue{" +
                "value='" + value + '\'' +
                '}';
    }
}
