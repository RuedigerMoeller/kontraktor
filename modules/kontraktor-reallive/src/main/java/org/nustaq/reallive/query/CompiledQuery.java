package org.nustaq.reallive.query;

import java.util.function.Supplier;

/**
 * Created by ruedi on 28/08/15.
 */
public class CompiledQuery {

    Supplier<Value> compiled;
    EvalContext[] ref;

    public CompiledQuery(Supplier<Value> compiled, EvalContext[] ref) {
        this.compiled = compiled;
        this.ref = ref;
    }

    public Value evaluate( EvalContext rec ) {
        ref[0] = rec;
        return compiled.get();
    }

}
