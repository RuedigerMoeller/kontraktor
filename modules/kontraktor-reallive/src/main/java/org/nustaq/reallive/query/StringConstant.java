package org.nustaq.reallive.query;

/**
 * Created by moelrue on 27.08.2015.
 */
public class StringConstant extends Operand {
    String value;

    public StringConstant(String value) {
        this.value = value;
    }

    @Override
    public Number getNumberValue() {
        return 0;
    }

    @Override
    public String getStringValue() {
        return value;
    }

}
