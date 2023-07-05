package org.nustaq.reallive.messages;

import org.nustaq.kontraktor.util.Pair;
import org.nustaq.reallive.api.Record;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by ruedi on 03/08/15.
 *
 */
public class Diff implements Serializable {

    final String changedFields[];
    final Object oldValues[]; // can be null if previous record is unknown (incoming change only, broadcast always fill this)

    public Diff(String[] changedFields, Object[] oldValues) {
        this.changedFields = changedFields;
        this.oldValues = oldValues;
    }

    public String[] getChangedFields() {
        return changedFields;
    }

    public Object[] getOldValues() {
        return oldValues;
    }

    /**
     * return wether field is in changedfieldlist and old value of this field
     * @param fieldId
     * @return
     */
    public Pair<Boolean,?> hasValueChanged(String fieldId) {
        for (int i = 0; i < changedFields.length; i++) {
            String changedField = changedFields[i];
            if ( fieldId.equals(changedField) ) {
                return new Pair<>(true,oldValues[i]);
            }
        }
        return new Pair<>(false,null);
    }

    @Override
    public String toString() {
        return "Diff{" +
                "changedFields=" + Arrays.toString(changedFields) +
                ", oldValues=" + Arrays.toString(oldValues) +
                '}';
    }

    public Diff omit(String[] fields) {
        ArrayList<Pair<String,Object>> newDiff = new ArrayList();
        HashSet toRemove = new HashSet(Arrays.asList(fields));
        for (int i = 0; i < changedFields.length; i++) {
            String changedField = changedFields[i];
            if ( ! toRemove.contains(changedField) )
                newDiff.add(new Pair(changedField,oldValues[i]));
        }
        String newChanged[] = new String[newDiff.size()];
        Object newVals[] = new Object[newDiff.size()];
        for (int i = 0; i < newDiff.size(); i++) {
            Pair<String, Object> stringObjectPair = newDiff.get(i);
            newChanged[i] = stringObjectPair.car();
            newVals[i] = stringObjectPair.cdr();
        }
        return new Diff(newChanged,newVals);
    }

    public Diff reduced(String[] reducedFields) {
        ArrayList<Pair<String,Object>> newDiff = new ArrayList();
        for (int i = 0; i < changedFields.length; i++) {
            String changedField = changedFields[i];
            for (int j = 0; j < reducedFields.length; j++) {
                String reducedField = reducedFields[j];
                if ( changedField.equals(reducedField) ) {
                    newDiff.add(new Pair(reducedField,oldValues[i]));
                }
            }
        }
        String newChanged[] = new String[newDiff.size()];
        Object newVals[] = new Object[newDiff.size()];
        for (int i = 0; i < newDiff.size(); i++) {
            Pair<String, Object> stringObjectPair = newDiff.get(i);
            newChanged[i] = stringObjectPair.car();
            newVals[i] = stringObjectPair.cdr();
        }
        return new Diff(newChanged,newVals);
    }

    public boolean isEmpty() {
        return changedFields!=null&&changedFields.length == 0;
    }

    public boolean containsField(String field) {
        if ( changedFields != null ) {
            for (int i = 0; i < changedFields.length; i++) {
                String changedField = changedFields[i];
                if ( field.equals(changedField ) )
                    return true;
            }
        }
        return false;
    }

    public void applyToOldRecord( Record oldRec, Record newRec ) {
        if ( changedFields != null ) {
            for (int i = 0; i < changedFields.length; i++) {
                String changedField = changedFields[i];
                oldRec.put(changedField,newRec.get(changedField));
            }
        }
    }

}
