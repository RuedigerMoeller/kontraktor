package org.nustaq.reallive.query;

/**
 * Created by moelrue on 27.08.2015.
 */
public class BoolOperand extends Operand {
    boolean val;

    @Override
    public Number getNumberValue() {
        return val ? 1 : 0;
    }

    @Override
    public String getStringValue() {
        return ""+val;
    }
}
