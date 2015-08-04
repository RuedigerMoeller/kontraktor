package org.nustaq.reallive.newimpl;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Created by ruedi on 03/08/15.
 */
public class ChangeUtils {

    public static Diff copyAndDiff(Record from, Record to) {
        String[] fields = from.getFields();
        return copyAndDiff(from, to, fields);
    }

    public static Diff copyAndDiff(Record from, Record to, String[] fields) {
        ArrayList<String> changedFields = new ArrayList<>();
        ArrayList<Object> changedValues = new ArrayList<>();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            Object oldValue = to.get(field);
            Object newValue = from.get(field);
            if ( ! Objects.equals(oldValue, newValue) ) {
                changedFields.add(field);
                changedValues.add(oldValue);
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
}
