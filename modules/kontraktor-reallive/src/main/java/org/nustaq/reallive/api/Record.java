package org.nustaq.reallive.api;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;
import org.nustaq.reallive.query.EvalContext;
import org.nustaq.reallive.query.LongValue;
import org.nustaq.reallive.query.StringValue;
import org.nustaq.reallive.query.Value;
import org.nustaq.reallive.records.MapRecord;
import org.nustaq.reallive.server.storage.RecordJsonifier;

import java.io.Serializable;
import java.util.*;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface Record extends Serializable, EvalContext {

    public static final String _NULL_ = "_NULL_";

    /**
     * create new json'ish record from key value array.
     *
     * some value types are transformed / replaced automatically to ensure
     * simple and easy-to-convert data structures
     *
     * - Map is transformed to Record
     * - a Collection is transformed to Object[]
     *
     * @param keyVals
     * @return
     */
    static Record from( Object ... keyVals ) {
        MapRecord aNew = MapRecord.New(null);
        for (int i = 0; i < keyVals.length; i+=2) {
            String key = (String) keyVals[i];
            Object val = keyVals[i+1];
            if ( key.equals("key") ) {
                aNew.key((String) val);
            } else {
                if (val == null)
                    val = _NULL_;
                aNew.internal_put(key, val);
            }
        }
        return aNew;
    }

    /**
     * transform map/collections into Record, Object[]
     *
     * - Map is transformed to Record
     * - a Collection is transformed to Object[]
     *
     * @param val
     * @return
     */
    static Object transform(Object val) {
        if ( val instanceof Map ) {
            return from((Map)val);
        }
        if ( val instanceof Collection ) {
            return ((Collection) val).toArray(new Object[((Collection) val).size()]);
        }
        return val;
    }

    static Record from( JsonObject jsonObject ) {
        return RecordJsonifier.get().toRecord(jsonObject);
    }

    /**
     * create a new record from given key and values
     *
     * @param map
     * @return
     */
    static Record from( Map<String,Object> map ) {
        return RecordJsonifier.get().from(map);
    }

    String getKey();
    long getLastModified();
    void internal_setLastModified(long tim);
    void internal_incSequence();
    long getSequence(); // increments with each update of the record
    Record internal_put(String key, Object value); // put without any prechecks / null processing

    default void internal_updateLastModified() {
        internal_setLastModified(System.currentTimeMillis());
        internal_incSequence();
    }

    /**
     * take care, kind of dangerous
     * @param key
     */
    Record key(String key);

    String[] getFields();

    default Set<String> getFieldSet() {
        String[] fields = getFields();
        HashSet res = new HashSet(fields.length);
        for (int i = 0; i < fields.length; i++) {
            res.add(fields[i]);
        }
        return res;
    }

    Record put( String field, Object value );

    @Override
    default Value getValue(String field) {
        if ( "_key".equals(field))
            return new StringValue( getKey(), null );
        if ( "_lastModified".equals(field))
            return new LongValue( getLastModified(), null );
        return EvalContext.super.getValue(field);
    }

    default Object mget( Object ... path ) {
        if ( path.length == 0 )
            return this;
        Object current = this;
        for( int i=0; i < path.length; i++) {
            Object index = path[i];
            if ( index instanceof String ) {
                if ( current instanceof Record ) {
                    current = ((Record) current).get((String) index);
                } else {
                    return null;
                }
            } else if ( index instanceof Number ) {
                if ( current instanceof Object[] ) {
                    current = ((Object[])current)[((Number) index).intValue()];
                } else
                    return null;
            }
        }
        return current;
    }

    default Number mgetNum( Object ... path ) {
        Object mget = mget(path);
        if ( mget instanceof Number )
            return (Number) mget;
        return 0;
    }


    default String mgetString( Object ... path ) {
        Object mget = mget(path);
        if ( mget == null )
            return "";
        return mget.toString();
    }

    default int getInt(String field) {
        Object val = get(field);
        if ( val == null )
            return 0;
        return ((Number)val).intValue();
    }

    default long getLong(String field) {
        Object val = get(field);
        if ( val == null )
            return 0;
        return ((Number)val).longValue();
    }

    /**
     * gets field and transforms Object[] to arraylist in case
     *
     * @param field
     * @return
     */
    default List asList( String field ) {
        Object val = get(field);
        if ( val instanceof Object[] ) {
            return new ArrayList(Arrays.asList((Object[])val));
        }
        else
            return null;
    }

    /**
     * gets field and transforms Object[] to HashSet in case
     *
     * @param field
     * @return
     */
    default Set asSet( String field ) {
        Object val = get(field);
        if ( val instanceof Object[] ) {
            return new HashSet(Arrays.asList((Object[])val));
        }
        else
            return null;
    }

    default Record putTransforming( String field, Object value ) {
        put(field,transform(value));
        return this;
    }

    /**
     * creates and sets an empty record in case
     * @param field
     * @return
     */
    default Record haveRec(String field) {
        Object val = get(field);
        if ( val == null ) {
            MapRecord aNew = MapRecord.New(null);
            put(field,aNew);
            return aNew;
        }
        return getRec(field);
    }

    /**
     * return sub-record in case present
     *
     * @param field
     * @return
     */
    default Record getRec(String field) {
        Object val = get(field);
        if ( val instanceof Record ) {
            return (Record) val;
        }
        return null;
    }

    /**
     * see asList
     * @param field
     * @return
     */
    default  <T> List<T> getAsList(String field ) {
        return asList(field);
    }

    default double getDouble(String field) {
        Object val = get(field);
        if ( val == null )
            return 0;
        return ((Number)val).doubleValue();
    }

    default String getString(String field) {
        Object val = get(field);
        if ( val == null )
            return null;
        return val.toString();
    }

    default String getSafeString(String field) {
        Object val = get(field);
        if ( val == null )
            return "";
        return val.toString();
    }

    default String asString() {
        String[] fields = getFields();
        String res = "[  *"+getKey()+"  ";
        for (int i = 0; i < fields.length; i++) {
            String s = fields[i];
            res += s+"="+get(s)+", ";
        }
        return res+"]";
    }

    default boolean getBool(String field) {
        Object val = get(field);
        if ( val instanceof Boolean == false )
            return false;
        return ((Boolean) val).booleanValue();
    }

    default Record reduced(String[] reducedFields) {
        MapRecord rec = MapRecord.New(getKey());
        for (int i = 0; i < reducedFields.length; i++) {
            String reducedField = reducedFields[i];
            Object val = get(reducedField);
            if ( val != null ) {
                rec.put(reducedField,val);
            }
        }
        return rec;
    }

    default Record omit(String ... fieldsToOmit) {
        MapRecord rec = MapRecord.New(getKey());
        HashSet<String> toOmit = new HashSet<>();
        for (int i = 0; i < fieldsToOmit.length; i++) {
            toOmit.add(fieldsToOmit[i]);
        }
        String[] fields = getFields();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            if ( !toOmit.contains(field) ) {
                Object val = get(field);
                if ( val != null ) {
                    rec.put(field,val);
                }
            }
        }
        return rec;
    }

    /**
     * @return a shallow (!) copy of this record
     */
    default Record shallowCopy() {
        throw new RuntimeException("copy not implemented");
    }
    default MapRecord deepCopied() {
        throw new RuntimeException("copy not implemented");
    }

    default Object[] getKeyVals() {
        final String[] fields = getFields();
        Object[] res = new Object[fields.length*2];
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            res[i*2] = field;
            res[i*2+1] = get(field);
        }
        return res;
    }

    /**
     * @return this record as a map
     */
    default Map<String,Object> asMap() {
        HashMap<String,Object> res = new HashMap<>();
        final String[] fields = getFields();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            res.put(field,get(field));
        }
        return res;
    }

    /**
     * copy all fields from given record to this
     * @param record
     */
    default void merge(Record record) {
        final String[] fields = record.getFields();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            put( field, record.get(field) );
        }
    }

    default Record getRecord() {
        return this;
    }

    /**
     * remove special operators on this record attributes
     */
    default void stripOps() {
        String[] fields = getFields();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            if ( field.endsWith("?+") ) {
                put( field.substring(0,field.length()-2), get(field) );
                put( field, null );
            } else if ( field.endsWith("+") || field.endsWith("-") ) {
                put( field.substring(0,field.length()-1), get(field) );
                put( field, null );
            }
        }
    }

    /**
     * a simple tree merge (without any operators like deepMerge)
     * copies all fields of given record to this.
     * if this contains a record for a field e.g.
     * this = { test: { a: 1, b: 2 } } and that = { test: { c: 1, b: 3 }} merging will be recursively
     * resulting in { test: { a: 1, b: 3, c: 1 } }.
     *
     * for arrays all elements are appended if they do not yet exist.
     * e.g. this = { test: [1,2,3] } and that = { test: [2,4,5] } will result in { test: [1,2,3,4,5] }
     *
     * @param that
     * @return
     */
    default Record join( Record that ) {
        final String[] fields = that.getFields();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            Object foreignValue = that.get(field);
            Object selfValue = get(field);
            if ( selfValue == null ) {
                if ( foreignValue instanceof Record ) {
                    put(field, Record.from().join((Record) foreignValue));
                } else
                    put(field, foreignValue);
            } else if ( selfValue instanceof Object[] ) {
                if ( foreignValue instanceof Object[] ) {
                    // append foreign if not there
                    List<Object> foreignList = Arrays.asList((Object[]) foreignValue);
                    List<Object> selfList = new ArrayList<>(Arrays.asList((Object[]) selfValue));
                    for (int j = 0; j < foreignList.size(); j++) {
                        Object o = foreignList.get(j);
                        if ( !selfList.contains(o) )
                            selfList.add(o);
                    }
                    put( field, selfList.toArray() );
                } else {
                    put( field, foreignValue );
                }
            } else if ( selfValue instanceof Record ) {
                if ( foreignValue instanceof Record )
                    put(field, ((Record) selfValue).deepMerge((Record) foreignValue));
                else
                    put( field, foreignValue );
            } else {
                put(field, foreignValue);
            }
        }
        return this;
    }

    /**
     * merge all fields from given record to this including nested structures.
     * Assumes pure json data types (String Number Boolean Object[] Record)
     *
     * attribute operators:
     * '+' - insert into array
     * '-' - remove from array
     * '?+' - insert if not present into array
     *
     * @param record
     */
    default Record deepMerge(Record record) {
        final String[] fields = record.getFields();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            String op = "";
            Object foreignValue = record.get(field);
            if ( field.endsWith("?+") ) {
                op = "?+"; // add if not exists
                field = field.substring(0,field.length()-2);
            } else if ( field.endsWith("+") ) {
                op = "+"; // add
                field = field.substring(0,field.length()-1);
            } else if ( field.endsWith("-") ) {
                op = "-"; // remove if exists
                field = field.substring(0,field.length()-1);
            }
            Object selfValue = get(field);
            if ( selfValue == null ) {
                if ( foreignValue instanceof Record ) {
                    put(field, Record.from().deepMerge((Record) foreignValue));
                } else
                    put(field, foreignValue);
            } else if ( selfValue instanceof Object[] ) {
                handleArrayOp(field, op, foreignValue, (Object[]) selfValue);
            } else if ( selfValue instanceof Record ) {
                if ( op.length() == 0 ) {
                    // no op => plain put
                    put(field, foreignValue);
                } else {
                    switch ( op ) {
                        case "-": {
                            throw new RuntimeException("inconsistent operator '"+op+" on type Record field "+field);
                        }
                        case "+": {
                            if ( foreignValue instanceof Record )
                                put(field, ((Record) selfValue).deepMerge((Record) foreignValue));
                            else
                                throw new RuntimeException("inconsistent operator '"+op+" on type Record field "+field);
                        } break;
                        case "?+": {
                            throw new RuntimeException("inconsistent operator '"+op+"' field "+field);
                        }
                        default:
                            throw new RuntimeException("unknown operator '"+op+"' on  type Record field "+field);
                    }
                }
            } else {
                put(field, foreignValue);
            }
        }
        return this;
    }

    private void handleArrayOp(String field, String op, Object foreignValue, Object[] selfValue) {
        // handle array ops
        if ( op.length() > 0 && foreignValue instanceof Object[] == false ) {
            // chosed operator, but did not provide array
            foreignValue = new Object[] { foreignValue };
        }
        switch ( op ) {
            case "-": {

            } break;
            case "+":
            case "?+": {
                // merge arrays
                Object[] foreignArr = (Object[]) foreignValue;
                Object[] selfArr = selfValue;
                ArrayList unmatched = new ArrayList();
                for (int jj = 0; jj < foreignArr.length; jj++) {
                    Object toAdd = foreignArr[jj];
                    boolean matched = false;
                    if ("?+".equals(op)) {
                        for (int j = 0; j < selfArr.length; j++) {
                            Object o1 = selfArr[j];
                            if (Objects.deepEquals(o1, toAdd)) {
                                matched = true;
                                selfArr[j] = toAdd;
                                break;
                            }
                        }
                    }
                    if (!matched) {
                        unmatched.add(toAdd);
                    }
                }
                if ( unmatched.size() > 0 ) {
                    List<Object> objects = new ArrayList(Arrays.asList(selfArr));
                    objects.addAll(unmatched);
                    selfArr = objects.toArray();
                    put( field,selfArr);
                } else {
                    put( field,selfArr);
                }
            } break;
            case "": {
                put(field, foreignValue);
            } break;
            default:
                throw new RuntimeException("unknown operator '"+op+"'");
        }
    }

    default String toPrettyString() {
        return RecordJsonifier.get().fromRecord(this ).toString(WriterConfig.PRETTY_PRINT);
    }

    default JsonObject toJson() {
        return RecordJsonifier.get().fromRecord(this);
    }

    default boolean validateForJsonability() {
        String[] fields = getFields();
        Class allowed[] = { Number.class, Boolean.class, Object[].class, String.class };
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            Object val = get(field);
            if ( val != null ) {
                boolean valid = false;
                if ( val instanceof Record ) {
                    valid = ((Record)val).validateForJsonability();
                } else {
                    for (int j = 0; j < allowed.length; j++) {
                        Class aClass = allowed[j];
                        if (aClass.isAssignableFrom(val.getClass())) {
                            valid = true;
                            break;
                        }
                    }
                    if ( ! valid )
                        System.err.println("invalid attribute value:"+val.getClass().getName()+" in field "+field);
                }
                if ( ! valid )
                    return false;
            }
        }
        return true;
    }

    default boolean defaultEquals( Object other ) {
        if ( other instanceof Record ) {
            Record oRec = (Record) other;
            if ( ! Objects.equals(getKey(),((Record) other).getKey()))
                return false;
            Set<String> fieldSet = getFieldSet();
            Set<String> oFieldSet = oRec.getFieldSet();
            if ( fieldSet.size()!=oFieldSet.size() )
                return false;
            for (Iterator<String> iterator = oFieldSet.iterator(); iterator.hasNext(); ) {
                String field = iterator.next();
                Object o = get(field);
                Object oOther = oRec.get(field);
                if ( o instanceof Number && oOther instanceof Number ) {
                    if ( ((Number) o).doubleValue() != ((Number) oOther).doubleValue() )
                        return false;
                } else if ( !Objects.deepEquals(o,oOther) ) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * called by the persistance layer after a record has been loaded from disk. can be used for type migration of an existing
     * database.
     * return true if the record should be re-persisted
     */
    default boolean _afterLoad() {
        // do nothing
        return false;
    }

    /**
     * tries to interpret the value as a long. In case its a string, its parsed.
     * @param key
     * @return
     */
    default long asLong(String key) {
        return getValue(key).getLongValue();
    }

    /**
     * tries to interpret the value as a double. In case its a string, its parsed.
     * @param key
     * @return
     */
    default double asDouble(String key) {
        return getValue(key).getDoubleValue();
    }

    default Object[] getArr(String z) {
        return (Object[]) get(z);
    }
}