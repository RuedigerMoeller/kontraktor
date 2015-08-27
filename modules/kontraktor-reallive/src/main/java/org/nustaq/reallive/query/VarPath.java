package org.nustaq.reallive.query;

/**
 * Created by moelrue on 27.08.2015.
 */
public class VarPath extends Operand {
    String value;

    public VarPath(String value) {
        this.value = value;
    }

    @Override
    public Number getNumberValue() {
        return null;
    }

    @Override
    public String getStringValue() {
        return null;
    }
}
