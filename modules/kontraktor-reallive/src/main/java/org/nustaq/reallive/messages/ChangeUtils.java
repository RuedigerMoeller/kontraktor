package org.nustaq.reallive.messages;

import org.nustaq.reallive.api.*;

import java.util.*;

import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.server.StorageDriver;

/**
 * Created by ruedi on 03/08/15.
 */
public class ChangeUtils {

    /**
     * compute deep differences between both records
     *
     * @param oldRec
     * @param newRec
     * @return a diff or null in case records are equal
     */
    public static Diff computeDiff(Record oldRec, Record newRec) {
        oldRec = StorageDriver.unwrap(oldRec);
        newRec = StorageDriver.unwrap(newRec);
        String[] fields = oldRec.getFields();
        Set<String> fieldSet = oldRec.getFieldSet();
        List<String> changed = new ArrayList<>();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            Object oldVal = oldRec.get(field);
            Object newVal = newRec.get(field);
            if (!Objects.deepEquals(oldVal,newVal) ) {
                changed.add(field);
            }
        }
        fields = newRec.getFields();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            if ( !fieldSet.contains(field) ) {
                Object oldVal = oldRec.get(field);
                Object newVal = newRec.get(field);
                if ( !Objects.deepEquals(oldVal,newVal) ) {
                    changed.add(field);
                }
            }
        }
        Object changedOldValues[] = new Object[changed.size()];
        for (int i = 0; i < changed.size(); i++) {
            changedOldValues[i] = oldRec.get(changed.get(i));
        }
        return new Diff(changed.toArray(new String[changed.size()]),changedOldValues);
    }

}
