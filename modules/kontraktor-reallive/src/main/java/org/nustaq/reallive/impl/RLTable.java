package org.nustaq.reallive.impl;

import org.nustaq.kontraktor.annotations.AsCallback;
import org.nustaq.offheap.bytez.ByteSource;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.reallive.*;
import org.nustaq.reallive.impl.storage.BinaryStorage;
import org.nustaq.reallive.impl.storage.FSTBinaryStorage;
import org.nustaq.reallive.sys.annotations.InMem;
import org.nustaq.reallive.sys.annotations.KeyLen;
import org.nustaq.reallive.sys.tables.SysTable;
import org.nustaq.serialization.FSTClazzInfo;

import java.io.File;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * Created by ruedi on 21.06.14.
 *
 * todo:
 * add CAS
 * add Client Object to be able to correlate changes and broadcasts
 * add striping
 */
public class RLTable<T extends Record> {

    public static int DEFAULT_TABLE_MEM_MB = 1;

    String tableId;
    IdGenerator<String> idgen;
    BinaryStorage<String,Record> storage;
    Class clazz;

    RealLive realLive; // shared
    private ChangeBroadcastReceiver receiver;
    boolean isShutDown;

    public void init( String tableId, RealLive realLive, Class<T> clz ) {
        Thread.currentThread().setName("TableImpl:"+tableId);
        this.clazz = clz;
        this.tableId = tableId;
        this.realLive = realLive;
        new File(realLive.getDataDirectory()).mkdirs();
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
                realLive.getDataDirectory() + File.separator + tableId+".mmf",
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
    }

    public Class getRowClazz() {
        return clazz;
    }

    public String getTableId() {
        return tableId;
    }

    public RealLive getRealLive() {
        return realLive;
    }

    public T createForAddWith(Class<? extends Record> clazz) {
        try {
            T res = (T) clazz.newInstance();
            res._setTable(this);
            res.setClazzInfo(getClazzInfo(res));
            res._setMode(Record.Mode.ADD);
            return res;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public T createForAdd() {
        return (T)createForAddWith(clazz);
    }

    public T createForAddWithKey(String key) {
        T forAdd = createForAdd();
        forAdd._setRecordKey(key);
        return forAdd;
    }

    public T createForUpdateWith(Class<? extends Record> clazz, String key, boolean addIfNotPresent) {
        T res = createForAddWith(clazz);
        res._setMode(addIfNotPresent ? Record.Mode.UPDATE_OR_ADD : Record.Mode.UPDATE);
        res._setRecordKey(key);
        T org = createForAdd();
        org._setRecordKey(key);
        res._setOriginalRecord(org);
        return res;
    }

    public T createForUpdate(String key, boolean addIfNotPresent) {
        return createForUpdateWith((Class<T>) clazz, key, addIfNotPresent);
    }

    public void prepareForUpdate(T record) {
        T res = null;
        try {
            res = (T) record.getClass().newInstance();
            res._setTable(this);
            if ( record.getClassInfo() == null )
                record.setClazzInfo(getClazzInfo(record));
            record._setMode(Record.Mode.UPDATE);
            record.copyTo(res);
            record._setOriginalRecord(res);
            record._setTable(this);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    //////////////////////////////////////////////////////////////////////
    //
    // mutation
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
        broadCastAdd(object,originator);
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
            record.setClazzInfo(getClazzInfo(record));
            newRec.setClazzInfo(getClazzInfo(newRec));
            RecordChange recordChange = newRec.computeDiff(record, true);
            update(recordChange, false);
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

    private FSTClazzInfo getClazzInfo(Record record) {
        return getRealLive().getConf().getClazzInfo(record.getClass());
    }

    public void update(RecordChange<String,T> change, boolean addIfNotPresent ) {
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

    public boolean updateCAS(RecordChange<String, T> change, Predicate<T> condition) {
        if (isShutDown)
            return false;
        T t = get(change.getId());
        if ( t == null ) {
            return false;
        }
        boolean success = condition.test(t);
        if ( success ) {
            update(change,false);
        }
        return true;
    }

    public void remove(String key, int originator) {
        if (isShutDown)
            return;
        Record record = storage.removeAndGet(key);
        if ( record != null )
            broadCastRemove(record,originator);
    }

//    @AsCallback
//    public void reportStats() {
//        if (isShutDown)
//            return;
//        final RLTable st = getRealLive().getTable("SysTable");
//        if (st!=null) {
//            SysTable sysTable = (SysTable) st.createForUpdate(tableId, true);
//            sysTable.setNumElems(storage.size());
//            sysTable.setSizeMB(storage.getSizeMB());
//            sysTable.setFreeMB(storage.getFreeMB());
//            sysTable.apply(0);
//            delayed(3000, () -> reportStats());
//        }
//    }

    //
    // mutation
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
                t._setTable(this);
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
        res._setTable(this);
        return res;
    }

    private void put(String key, T object) {
        if (isShutDown)
            return;
        storage.put(key, object);
    }

}
