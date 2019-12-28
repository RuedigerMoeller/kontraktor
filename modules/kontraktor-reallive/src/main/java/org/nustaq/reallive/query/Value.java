package org.nustaq.reallive.query;

import java.io.Serializable;

/**
 * Created by moelrue on 27.08.2015.
 */
public interface Value extends Serializable, HasToken {
    Value TRUE = new LongValue(1, null);
    Value FALSE = new LongValue(0, null);

    QToken getToken();
    double getDoubleValue();
    long getLongValue();
    String getStringValue();

    default Object[] getArrayValue() {
        return new Object[] { getValue() };
    }

    Object getValue();

    default boolean isDouble() {
        return this instanceof DoubleValue;
    }

    default boolean isLong() {
        return this instanceof LongValue;
    }

    default boolean isString() {
        return this instanceof StringValue;
    }

    default boolean isArray() { return this instanceof ArrayValue; }

    default boolean isTrue() {
        if (isString()) {
            String sv = getStringValue();
            return !"false".equals(sv) && sv.length() > 0 && !"0".equals(sv);
        }
        return getLongValue() != 0;
    }

    Value negate();

    boolean isEmpty();
}
