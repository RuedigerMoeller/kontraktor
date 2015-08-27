package org.nustaq.reallive.query;

/**
 * Created by moelrue on 27.08.2015.
 */
public class FuncOperand extends Operand {

    String name;

    public FuncOperand(String name) {
        this.name = name;
    }

    @Override
    public Number getNumberValue() {
        return null;
    }

    @Override
    public String getStringValue() {
        return name;
    }
}
