package org.nustaq.reallive.messages;

import org.nustaq.reallive.api.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import org.nustaq.reallive.api.Record;

/**
 * Created by ruedi on 03/08/15.
 */
public class ChangeUtils {

    /**
     * copy different (equals) fields and return resulting list of changed fields + old values
     *
     * @param from
     * @param to
     * @return
     */
    public static Diff copyAndDiff(Record from, Record to) {
        String[] fields = from.getFields();
        return copyAndDiff(from, to, fields);
    }

    public static Diff copyAndDiff(Record from, Record to, String[] fields) {
        return mayCopyAndDiff(from, to, fields, true, null);
    }

    public static Diff copyAndDiff(Record from, Record to, String[] fields, HashSet<String> forced) {
        return mayCopyAndDiff(from, to, fields, true, forced);
    }

    public static Diff mayCopyAndDiff(Record from, Record to, String[] fields, boolean copy, HashSet<String> forced) {
        ArrayList<String> changedFields = new ArrayList();
        ArrayList<Object> changedValues = new ArrayList();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            Object oldValue = to.get(field);
            Object newValue = from.get(field);

            if ( ! Objects.equals(oldValue, newValue) || (forced != null && forced.contains(field) )) {
                changedFields.add(field);
                changedValues.add(oldValue);
                if ( copy )
                    to.put(field, newValue);
            }
        }
        String cf[] = new String[changedFields.size()];
        changedFields.toArray(cf);
        return new Diff(cf,changedValues.toArray());
    }

    public static int indexOf(String field, String[] changedFields) {
        for (int i = 0; i < changedFields.length; i++) {
            if ( field.equals(changedFields[i]) ) {
                return i;
            }
        }
        return -1;
    }

    public static void copy(Record from, Record to, String ... fields) {
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            to.put(field,from.get(field));
        }
    }

    public static String[] merge( String fieldsA[], String fieldsB[] ) {
        HashSet set = new HashSet();
        for (int i = 0; i < fieldsA.length; i++) {
            set.add(fieldsA[i]);
        }
        for (int i = 0; i < fieldsB.length; i++) {
            set.add(fieldsB[i]);
        }
        String res[] = new String[set.size()];
        set.toArray(res);
        return res;
    }

    public static <K> Diff diff(Record record, Record prevRecord) {
        String[] fields = merge(record.getFields(), prevRecord.getFields());
        return mayCopyAndDiff(record,prevRecord,fields,false, null);
    }

}
