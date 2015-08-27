package org.nustaq.reallive.query;

/**
 * Created by moelrue on 27.08.2015.
 */
public class NumberOperand extends Operand {

    Number value;

    public NumberOperand(Number value) {
        this.value = value;
    }

    public Number getNumberValue() {
        if ( value == null )
            return 0;
        return value;
    }

    @Override
    public String getStringValue() {
        return ""+getNumberValue();
    }

}
