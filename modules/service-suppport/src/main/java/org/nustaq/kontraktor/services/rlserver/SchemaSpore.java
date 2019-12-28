package org.nustaq.kontraktor.services.rlserver;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.Spore;
import org.nustaq.reallive.api.RealLiveTable;
import org.nustaq.reallive.api.Record;

import java.util.*;

public class SchemaSpore extends Spore<Record, Set<String>> {

    Set<String> fieldSet = new HashSet<>();

    public SchemaSpore() {
    }

    @Override
    public void remote(Record input) {
        String[] fields = input.getFields();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            fieldSet.add(field);
        }
    }

    @Override
    public void finish() {
        stream(fieldSet);
        super.finish();
    }

    public static void merge(Set<String> result,Set<String> other) {
        result.addAll(other);
    }

    public static IPromise<Set<String>> apply(RealLiveTable tbl) {
        Promise res = new Promise();
        HashSet<String> set = new HashSet<>();
        tbl.forEach(new SchemaSpore(), (r,e) -> {
            if ( r != null ) {
                merge(set,r);
            } else {
                res.resolve(set);
            }
        });
        return res;
    }
}
