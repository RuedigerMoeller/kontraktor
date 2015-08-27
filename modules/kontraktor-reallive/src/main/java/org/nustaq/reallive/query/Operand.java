package org.nustaq.reallive.query;

/**
 * Created by moelrue on 27.08.2015.
 */
public abstract class Operand extends Token {

    public abstract Number getNumberValue();
    public abstract String getStringValue();

}
