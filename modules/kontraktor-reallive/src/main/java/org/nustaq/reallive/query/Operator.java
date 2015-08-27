package org.nustaq.reallive.query;

/**
 * Created by moelrue on 27.08.2015.
 */
public class Operator extends Token {

    int order = 10; // low = low precedence, +- < */
    String value;

    public Operator(String value) {
        this.value = value;
    }

    public Operator(String value, int order) {
        this.order = order;
        this.value = value;
    }

    public String getString() {
        return value;
    }

    public int getPrecedence() {
        return order;
    }

}
