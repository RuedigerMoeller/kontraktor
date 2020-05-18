package org.nustaq.reallive.query;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by moelrue on 27.08.2015.
 */
public class VarPath implements Serializable, HasToken  {

    String field;
    String fields[];
    EvalContext ctx[];
    QToken token;
    String path;

    public VarPath(String field, EvalContext[] ctx, QToken token) {
        this.field = field;
        this.ctx = ctx;
        this.token = token;
        if ( field.indexOf('.') >= 0 ) {
            fields = field.split("\\.");
            path = field;
            if ( fields.length > 0 )
                this.field = fields[0];
            else {
                int debug = 1;
            }
        }
    }

    public String getPath() {
        return path;
    }

    public RLSupplier<Value> getEval() {
        return new SupplierWithToken<Value>() {
            @Override
            public Value get() {
                EvalContext ectx = ctx[0];
                return evaluate(ectx);
            }

            @Override
            public QToken getToken() {
                return VarPath.this.getToken();
            }
        };
    }

    public Value evaluate(EvalContext ectx) {
        if ( ectx != null ) {
            if ( fields != null && fields.length > 0 ) {
                EvalContext currentRec = null;
                for (int i = 0; i < fields.length-1; i++) {
                    Object o = ectx.get(fields[i]);
                    if ( o instanceof EvalContext == false )
                        return null;
                    currentRec = (EvalContext) o;
                }
                if ( currentRec != null ) {
                    return currentRec.getValue(fields[fields.length-1]);
                }
                return null;
            } else {
                return ectx.getValue(field);
            }
        }
        return null;
    }

    public QToken getToken() {
        if (token != null)
            return token;
        Value value = getEval().get();
        if ( value != null ) {
            return value.getToken();
        }
        return null;
    }
}

interface SupplierWithToken<T> extends RLSupplier<T>, HasToken {
}