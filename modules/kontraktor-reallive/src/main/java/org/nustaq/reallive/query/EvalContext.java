package org.nustaq.reallive.query;

/**
 * Created by ruedi on 29/08/15.
 */
public interface EvalContext {

    Object get(String field);
    default Value getValue(String field) {
        Object val = get(field);
        if ( val == null )
            val = "";
        if ( val instanceof String) {
            return new StringValue((String) val, null);
        } else if ( val instanceof Float || val instanceof Double) {
            return new DoubleValue(((Number) val).doubleValue(), null);
        } else if ( val instanceof Number ) {
            return new LongValue(((Number) val).longValue(), null);
        } else {
            return new StringValue(val.toString(), null);
        }
    }

}
