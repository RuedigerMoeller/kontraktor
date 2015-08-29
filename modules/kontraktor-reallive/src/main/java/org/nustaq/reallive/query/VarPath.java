package org.nustaq.reallive.query;

import java.io.Serializable;

/**
 * Created by moelrue on 27.08.2015.
 */
public class VarPath implements Serializable {
    String field;
    EvalContext ctx[];
    private RLSupplier<Value> eval;

    public VarPath(String field, EvalContext[] ctx) {
        this.field = field;
        this.ctx = ctx;
    }

    public RLSupplier<Value> getEval() {
        return () -> ctx[0].getValue(field);
    }
}
