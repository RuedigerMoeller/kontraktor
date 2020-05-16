package org.nustaq.reallive.impl.storage;

import org.nustaq.kontraktor.Spore;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.api.*;
import org.nustaq.reallive.api.Record;

import java.util.*;

/**
 * acts as a passive listener to maintain state
 *
 * Consider: null indexHashValues need not to be tracked as a separate bucket, => fallback to full scan then ?
 */
public class HashIndex implements StorageIndex {

    RLFunction<Record,Object> hashGetter;
    HashMap<Object, Set<String>> index = new HashMap<>();
    HashMap key2HashVal = new HashMap();

    public HashIndex(RLFunction<Record, Object> hashGetter) {
        this.hashGetter = hashGetter;
    }

    @Override
    public void put(String key, Record value) {
        Object hkey = hashGetter.apply(value);
        if ( hkey == null )
            hkey = "_NULL_";
        Object oldHKey = key2HashVal.get(key);
        if ( hkey.equals(oldHKey) )
            return;
        // remove
        index.get(oldHKey).remove(key);
        key2HashVal.put(key,hkey);
        Set<String> strings = index.get(hkey);
        if ( strings == null ) {
            strings = new HashSet<>();
            index.put(hkey,strings);
        }
        strings.add(key);
    }

    @Override
    public void remove(String key) {
        Object oldHVal = key2HashVal.get(key);
        index.get(oldHVal).remove(key);
        key2HashVal.remove(key);
    }

    @Override
    public <T> void forEachWithSpore(Object hashValue, Spore<Record, T> spore, RecordStorage store) {
        Set<String> strings = index.get(hashValue);
        if ( strings == null ) {
            spore.finish();
            return;
        }
        for (Iterator<String> iterator = strings.iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            Record record = store.get(key);
            if ( record == null )
                Log.Error(this,"inconsistent index, no record found for "+key);
            try {
                spore.remote(record);
            } catch ( Throwable ex ) {
                Log.Warn(this, ex, "exception in spore " + spore);
                throw ex;
            }
            if ( spore.isFinished() )
                break;
        }
        spore.finish();
    }
}
