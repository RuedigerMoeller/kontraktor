package org.nustaq.reallive.query;

/**
 * Created by ruedi on 27.08.2015.
 */
public class DoubleValue implements Value {

    double value;

    public DoubleValue(double value) {
        this.value = value;
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
    public Value negate() {
        return new DoubleValue(value*-1);
    }

    @Override
    public String toString() {
        return "DoubleValue{" +
                "value=" + value +
                '}';
    }
}
