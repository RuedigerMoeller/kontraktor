package org.nustaq.reallive.query;

import java.io.Serializable;

/**
 * Created by moelrue on 27.08.2015.
 */
public class FuncOperand implements Serializable {

    String name;
    int arity;

    public FuncOperand(String name, int arity) {
        this.name = name;
        this.arity = arity;
    }

    public int getArity() {
        return arity;
    }

    public RLSupplier<Value> getEval(RLSupplier<Value> args[]) {
        if ( args.length != arity )
            throw new QParseException("invalid number of arguments:"+name);
        return () -> apply(args);
    }

    protected Value apply(RLSupplier<Value>[] args) {
        return null;
    }
}
