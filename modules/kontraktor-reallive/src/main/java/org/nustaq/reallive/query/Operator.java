package org.nustaq.reallive.query;

import java.util.function.Supplier;

/**
 * Created by moelrue on 27.08.2015.
 */
public class Operator {
    int order = 10; // low = low precedence, +- < */
    String name;
    int arity = 2;

    public Operator(String name) {
        this.name = name;
    }

    public Operator(String name, int order) {
        this.order = order;
        this.name = name;
    }

    public Operator(String name, int order, int arity) {
        this.order = order;
        this.name = name;
        this.arity = arity;
    }

    public int getArity() {
        return arity;
    }

    public String getString() {
        return name;
    }

    public int getPrecedence() {
        return order;
    }

    public Supplier<Value> getEval( Supplier<Value> arg, Supplier<Value> arg1 ) {
        return () -> {
            Value vb = arg.get();
            Value va = arity > 1 ? arg1.get() : null;
            return compare(vb, va);
        };
    }

    protected Value compare(Value vb, Value va) {
        if ( va.isString() || vb.isString() ) {
            return new StringValue(stringOp(va.getStringValue(),vb.getStringValue()));
        }
        if ( va.isDouble() || vb.isDouble() ) {
            return new DoubleValue(doubleOp(va.getDoubleValue(), vb.getDoubleValue()));
        }
        return new LongValue(longOp(va.getLongValue(),vb.getLongValue()));
    }

    protected long longOp(long longValue, long longValue1) {
        return 0;
    }

    protected double doubleOp(double doubleValue, double doubleValue1) {
        return 0;
    }

    protected String stringOp(String stringValue, String stringValue1) {
        return null;
    }

}
