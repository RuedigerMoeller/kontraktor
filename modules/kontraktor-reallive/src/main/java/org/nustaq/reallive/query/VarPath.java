package org.nustaq.reallive.query;

import java.util.function.Supplier;

/**
 * Created by moelrue on 27.08.2015.
 */
public class VarPath {
    String field;
    EvalContext ctx[];
    private Supplier<Value> eval;

    public VarPath(String field, EvalContext[] ctx) {
        this.field = field;
        this.ctx = ctx;
    }

    public Supplier<Value> getEval() {
        return () -> ctx[0].getValue(field);
    }
}
