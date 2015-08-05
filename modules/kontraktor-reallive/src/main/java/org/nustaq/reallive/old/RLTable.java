package org.nustaq.reallive.old;

import org.nustaq.offheap.bytez.ByteSource;
import org.nustaq.kontraktor.Callback;
import org.nustaq.reallive.old.storage.BinaryStorage;
import org.nustaq.reallive.old.storage.FSTBinaryStorage;
import org.nustaq.reallive.old.sys.annotations.InMem;
import org.nustaq.reallive.old.sys.annotations.KeyLen;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTConfiguration;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Created by ruedi on 21.06.14.
 *
 */
public class RLTable<T extends Record> {

    public static int DEFAULT_TABLE_MEM_MB = 1;

    String tableId;
    IdGenerator<String> idgen;
    BinaryStorage<String,Record> storage;
    Class recordClass;

    private ChangeBroadcastReceiver receiver;
    boolean isShutDown;
    RecordContext context;
    FSTConfiguration conf;

    public void init( String tableId, String dataDirectoy, Class<T> clz, FSTConfiguration conf ) {
        Thread.currentThread().setName("TableImpl:"+tableId);
        this.recordClass = clz;
        this.tableId = tableId;
        new File(dataDirectoy).mkdirs();
        idgen = new StringIdGen("");
        try {
            FSTBinaryStorage<Record> recordFSTBinaryStorage = new FSTBinaryStorage<>();
            storage = recordFSTBinaryStorage;
            int keyLen = 16;
            KeyLen ks = clz.getAnnotation(KeyLen.class);
            if ( ks != null ) {
                keyLen = Math.max(ks.value(),keyLen);
            }
            InMem inMem = clz.getAnnotation(InMem.class);
            recordFSTBinaryStorage.init(
                    dataDirectoy + File.separator + tableId+".mmf",
                DEFAULT_TABLE_MEM_MB, // 1 GB init size
                100000, // num records
                keyLen, // keylen
                inMem == null,
                clz
             );
            idgen.setState(storage);
        } catch (Exception e) {
            e.printStackTrace();
        }
        context = new MyRecordContext(conf.getClassInfo(recordClass).getFieldInfo(),tableId);
    }

    public Class getRowClazz() {
        return recordClass;
    }

    public String getTableId() {
        return tableId;
    }

    T createForAddWith(Class<? extends Record> clazz) {
        try {
            T res = (T) clazz.newInstance();
            res._setTable(getRecordContext());
            return res;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    T createForAdd() {
        return (T)createForAddWith(recordClass);
    }

    private RecordContext getRecordContext() {
        return null;
    }

    static class MyRecordContext implements RecordContext, Serializable {
        FSTClazzInfo.FSTFieldInfo[] fieldInfo;
        String tableId;
        transient ConcurrentHashMap<String, FSTClazzInfo.FSTFieldInfo> cache;

        public MyRecordContext(FSTClazzInfo.FSTFieldInfo[] fieldInfo, String tableId) {
            this.fieldInfo = fieldInfo;
            this.tableId = tableId;
        }

        @Override
        public FSTClazzInfo.FSTFieldInfo[] getFieldInfo() {
            return fieldInfo;
        }

        @Override
        public FSTClazzInfo.FSTFieldInfo getFieldInfo(String fieldName) {
            if ( cache == null ) {
                cache = new ConcurrentHashMap<>();
                Arrays.stream(fieldInfo).forEach( field -> cache.put(field.getName(),field));
                return cache.get(fieldName);
            }
            return cache.get(fieldName);
        }

        @Override
        public String getTableId() {
            return tableId;
        }
    }

    //////////////////////////////////////////////////////////////////////
    //
    // mutation with broadcasting
    //

    public String addGetId(T object, int originator) {
        if (isShutDown)
            return null;
        if ( object.getRecordKey() == null ) {
            String nextKey = idgen.nextid();
            object._setRecordKey(nextKey);
        }
        put(object.getRecordKey(), object);
        broadCastAdd(object,originator);
        return object.getRecordKey();
    }

    public void add(T object, int originator) {
        if (isShutDown)
            return;
        if ( object.getRecordKey() == null ) {
            String nextKey = idgen.nextid();
            object._setRecordKey(nextKey);
        }
        put(object.getRecordKey(), object);
        broadCastAdd(object, originator);
    }

    public void put(String key, T newRec, int originator) {
        if (isShutDown)
            return;
        if ( newRec == null ) {
            remove(key,originator);
            return;
        }
        Record record = storage.get(key);
        newRec._setRecordKey(key);
        if ( record != null ) {
            RecordChange recordChange = newRec.computeDiff(record, true);
            update(recordChange,false);
        } else {
            storage.put(key, newRec);
            broadCastAdd(newRec, originator);
        }
    }

    public void putIfAbsent(String key, T object, int originator) {
        if ( storage.get(key) == null ) {
            put(key, object, originator);
        }
    }

    public T putIfAbsentWithResult(String key, T object, int originator) {
        T record = (T) storage.get(key);
        if ( record == null ) {
            put(key,object,originator);
            return null;
        }
        return record;
    }

    public void update(RecordChange<String, T> change, boolean addIfNotPresent) {
        if (isShutDown)
            return;
        T t = get(change.getId());
        if ( t != null ) {
            RecordChange appliedChange = change.apply(t);
            t.incVersion();
            put(t.getRecordKey(), t);
            broadCastUpdate(appliedChange, t);
        } else if (addIfNotPresent) {
            t = createForAdd();
            RecordChange appliedChange = change.apply(t);
            put(t.getRecordKey(), t);
            broadCastAdd(t,change.getOriginator());
        }
    }

    public void remove(String key, int originator) {
        if (isShutDown)
            return;
        Record record = storage.removeAndGet(key);
        if ( record != null )
            broadCastRemove(record,originator);
    }

    //
    // mutation with broadcast generation
    //
    //////////////////////////////////////////////////////////////////////

    private void broadCastRemove(Record rec, int originator) {
        if ( receiver != null )
            receiver.onChangeReceived(ChangeBroadcast.NewRemove(tableId, rec, originator));
    }

    private void broadCastAdd(T t, int originator) {
        if ( receiver != null )
            receiver.onChangeReceived(ChangeBroadcast.NewAdd(tableId, t, originator));
    }

    private void broadCastUpdate(RecordChange appliedChange, T newRecord) {
        if ( receiver != null )
            receiver.onChangeReceived(ChangeBroadcast.NewUpdate(tableId, newRecord, appliedChange));
    }


    public String nextKey() {
        return idgen.nextid();
    }

    public void shutDown() {
        isShutDown = true;
        storage.close();
    }

    public int getKeyLen() {
        return storage.getKeyLen();
    }


    public void filter(Predicate<T> doProcess, Predicate<T> terminate, Callback<T> resultReceiver) {
        if (isShutDown)
            return;
        Iterator<Record> vals = storage.values();
        while( vals.hasNext() ) {
            try {
                T t = (T) vals.next();
                t._setTable(getRecordContext());
                if (doProcess == null || doProcess.test(t)) {
                    resultReceiver.complete(t, null);
                }
                if (terminate != null && terminate.test(t))
                    break;
            } catch (Exception e ) {
                resultReceiver.complete(null, e);
            }
        }
        resultReceiver.finish();
    }

    public void filterBinary(Predicate<ByteSource> doProcess, Predicate<ByteSource> terminate, Callback resultReceiver) {
        if (isShutDown)
            return;
        Iterator<ByteSource> entries = storage.binaryValues();

        while( entries.hasNext() ) {
            try {
                ByteSource t = entries.next();
                if (doProcess == null || doProcess.test(t)) {
                    resultReceiver.complete(t, null);
                }
                if (terminate != null && terminate.test(t))
                    break;
            } catch (Exception e ) {
                resultReceiver.complete(null, e);
            }
        }
        resultReceiver.finish();
    }

    private T get(String key) {
        if (isShutDown)
            return null;
        T res = (T) storage.get(key);
        if ( res == null )
            return null;
        res._setTable(getRecordContext());
        return res;
    }

    private void put(String key, T object) {
        if (isShutDown)
            return;
        storage.put(key, object);
    }

}
