package org.nustaq.reallive.server.storage;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.nustaq.kontraktor.Spore;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.api.*;
import org.nustaq.reallive.api.Record;

import java.util.*;
import java.util.stream.Stream;

/**
 * acts as a passive listener to maintain state
 *
 * Consider: null indexHashValues need not to be tracked as a separate bucket, => fallback to full scan then ?
 */
public class HashIndex implements StorageIndex {

    private final String hashPath;
    RLFunction<Record,Object> hashGetter;
    Object2ObjectOpenHashMap<Object, Set<String>> index = new Object2ObjectOpenHashMap<>();
    Object2ObjectOpenHashMap<String,Object> key2HashVal = new Object2ObjectOpenHashMap<>();

    public HashIndex(RLFunction<Record, Object> hashGetter, String hashPath) {
        this.hashGetter = hashGetter;
        this.hashPath = hashPath;
    }

    public String getHashPath() {
        return hashPath;
    }

    public RLFunction<Record, Object> getHashGetter() {
        return hashGetter;
    }

    public Map<Object, Set<String>> getIndex() {
        return index;
    }

    public Map<String,Object> getKey2HashVal() {
        return key2HashVal;
    }

    @Override
    public void put(String key, Record value) {
        Object hkey = unifyKey(hashGetter.apply(value));
        Object oldHKey = key2HashVal.get(key);
        if ( hkey.equals(oldHKey) )
            return;
        // remove
        if ( oldHKey != null )
            index.get(oldHKey).remove(key);
        key2HashVal.put(key,hkey);
        Set<String> strings = index.get(hkey);
        if ( strings == null ) {
            strings = new HashSet<>();
            index.put(hkey,strings);
        }
        strings.add(key);
    }

    public static Object unifyKey(Object key) {
        if ( key instanceof Byte ||
            key instanceof Short ||
            key instanceof Integer ||
            key instanceof Long )
            return ((Number) key).longValue();
        if ( key instanceof Float || key instanceof Double )
            return ((Number) key).doubleValue();
        if ( key == null )
            return "_NULL_";
        return key;
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

    public static void main(String[] args) {
        HashMap mp = new HashMap();
        mp.put( unifyKey(Byte.valueOf((byte) 125)),"Hello");
        System.out.println(mp.get(unifyKey(125))+" "+mp.get(unifyKey(Long.valueOf(125))));
    }

    public Stream<String> getKeys(Object key) {
        return getKeySet(key).stream();
    }

    public Set<String> getKeySet(Object key) {
        Set<String> strings = index.get(unifyKey(key));
        if ( strings == null ) {
            return Collections.<String>emptySet();
        }
        return strings;
    }
}
