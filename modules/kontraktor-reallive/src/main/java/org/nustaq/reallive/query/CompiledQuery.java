package org.nustaq.reallive.query;

import java.io.Serializable;

/**
 * Created by ruedi on 28/08/15.
 */
public class CompiledQuery implements Serializable {

    RLSupplier<Value> compiled;
    EvalContext[] ref;

    public CompiledQuery(RLSupplier<Value> compiled, EvalContext[] ref) {
        this.compiled = compiled;
        this.ref = ref;
    }

    public Value evaluate( EvalContext rec ) {
        ref[0] = rec;
        return compiled.get();
    }

    public String getHashIndex() {
        return null;
    }

}
