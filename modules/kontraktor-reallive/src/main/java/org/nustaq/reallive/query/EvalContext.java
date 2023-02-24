package org.nustaq.reallive.query;
import org.nustaq.reallive.api.Record;
/**
 * Created by ruedi on 29/08/15.
 */
public interface EvalContext {

    Object get(String field);
    default Value getValue(String field) {
        Object val = get(field);
        if ( val == null ) {
            return NullValue.NULL;
        } else if ( val instanceof String) {
            return new StringValue((String) val, null);
        } else if ( val instanceof Float || val instanceof Double) {
            return new DoubleValue(((Number) val).doubleValue(), null);
        } else if ( val instanceof Number ) {
            return new LongValue(((Number) val).longValue(), null);
        } else if ( val instanceof Object[] ) {
            return new ArrayValue((Object[]) val, null);
        } else if ( val instanceof Record ) {
            return new RecordValue((Record) val);
        } else {
            return new StringValue(val.toString(), null);
        }
    }

}
