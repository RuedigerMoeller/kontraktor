package org.nustaq.reallive.query;

import org.nustaq.reallive.api.RLHashIndexPredicate;

import java.io.Serializable;

/**
 * Created by ruedi on 28/08/15.
 */
public class CompiledQuery implements Serializable {

    RLSupplier<Value> compiled;
    EvalContext[] ref;
    RLHashIndexPredicate hashIndex;

    public CompiledQuery(RLSupplier<Value> compiled, EvalContext[] ref) {
        this.compiled = compiled;
        this.ref = ref;
    }

    public Value evaluate( EvalContext rec ) {
        ref[0] = rec;
        return compiled.get();
    }

    public RLHashIndexPredicate getHashIndex() {
        return hashIndex;
    }

    public CompiledQuery hashIndex(RLHashIndexPredicate h) {
        hashIndex = h;
        return this;
    }

}
