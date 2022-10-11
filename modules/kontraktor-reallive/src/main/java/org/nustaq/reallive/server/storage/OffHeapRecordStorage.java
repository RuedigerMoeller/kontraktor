package org.nustaq.reallive.server.storage;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Spore;
import org.nustaq.kontraktor.impl.DispatcherThread;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.offheap.FSTAsciiStringOffheapMap;
import org.nustaq.offheap.FSTBinaryOffheapMap;
import org.nustaq.offheap.FSTSerializedOffheapMap;
import org.nustaq.reallive.api.*;
import org.nustaq.reallive.server.FilebasedRemoveLog;
import org.nustaq.reallive.server.RemoveLog;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.simpleapi.DefaultCoder;
import org.nustaq.serialization.simpleapi.FSTCoder;
import org.nustaq.serialization.util.FSTUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.nustaq.reallive.api.Record;


/**
 * Created by moelrue on 05.08.2015.
 */
public class OffHeapRecordStorage implements RecordStorage {

    private static final boolean DEBUG = false;
    OutputStream protocol;
    FSTCoder coder;

    FSTSerializedOffheapMap<String,Record> store;
    int keyLen;
    private String tableFile;
    private RemoveLog removeLog;

    protected OffHeapRecordStorage() {}

    public OffHeapRecordStorage(int maxKeyLen, int sizeMB, int estimatedNumRecords) {
        keyLen = maxKeyLen;
        init(null, sizeMB, estimatedNumRecords, maxKeyLen, false, Record.class );
    }

    public OffHeapRecordStorage(String file, int maxKeyLen, int sizeMB, int estimatedNumRecords) {
        keyLen = maxKeyLen;
        if ( DEBUG ) {
            try {
                protocol = new FileOutputStream(new File(file+"_prot") );
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        init(file, sizeMB, estimatedNumRecords, maxKeyLen, true, Record.class );
    }

    protected void init(String tableFile, int sizeMB, int estimatedNumRecords, int keyLen, boolean persist, Class... toReg) {
        this.keyLen = keyLen;
        this.tableFile = tableFile;
        coder = new DefaultCoder();
        if ( toReg != null )
            coder.getConf().registerClass(toReg);
        if ( persist ) {
            try {
                store = createPersistentMap(tableFile, sizeMB, estimatedNumRecords, keyLen);
                Thread thread = Thread.currentThread();
                if ( thread instanceof DispatcherThread) {
                    removeLog = Actors.AsActor(FilebasedRemoveLog.class, ((DispatcherThread) thread).getScheduler());
                } else {
                    removeLog = Actors.AsActor(FilebasedRemoveLog.class);
                }
                File file = new File(tableFile);
                File baseDir = file.getParentFile();
                String tableName = file.getName().substring(0,file.getName().lastIndexOf('.'));
                ((FilebasedRemoveLog)removeLog).init(baseDir.getAbsolutePath(),tableName);
            } catch (Exception e) {
                FSTUtil.rethrow(e);
            }
        }
        else
            store = createMemMap(sizeMB, estimatedNumRecords, keyLen);
    }

    protected FSTSerializedOffheapMap<String,Record> createMemMap(int sizeMB, int estimatedNumRecords, int keyLen) {
        return new FSTAsciiStringOffheapMap<Record>(keyLen, FSTBinaryOffheapMap.MB*sizeMB,estimatedNumRecords, coder);
    }

    protected FSTSerializedOffheapMap<String,Record> createPersistentMap(String tableFile, int sizeMB, int estimatedNumRecords, int keyLen) throws Exception {
        boolean fileExists = new File(tableFile).exists();
        try {
            return new FSTAsciiStringOffheapMap(tableFile, keyLen, FSTBinaryOffheapMap.MB * sizeMB, estimatedNumRecords, coder);
        } catch (Exception e) {
            Log.Error(this,e);
            Log.Warn(this, "exception when trying to mount table file, will backup and recover");

            if ( fileExists ) {
                try {
                    String corruptCopy = tableFile + "_corrupted";
                    Path copied = Paths.get(corruptCopy);
                    Path originalPath = Paths.get(tableFile);
                    Files.copy(originalPath, copied, StandardCopyOption.REPLACE_EXISTING);
                    Files.delete(originalPath);
                    return createPersistentMap(tableFile, sizeMB, estimatedNumRecords, keyLen);
                } catch (Exception ee) {
                    Log.Error(this, "failed to copy/delete corrupt file");
                    Log.Error(this, ee);
                }
            }
        }
        return null;
    }

    public StorageStats getStats() {
        StorageStats stats = new StorageStats()
            .name(store.getFileName())
            .capacity(store.getCapacityMB())
            .freeMem(store.getFreeMem())
            .usedMem(store.getUsedMem())
            .numElems(store.getSize());
        return stats;
    }

    @Override
    public RecordStorage put(String key, Record value) {
        value.internal_updateLastModified();
        return _put(key,value);
    }

    public RecordStorage _put(String key, Record value) {
        if ( protocol != null ) {
            try {
                FSTConfiguration.getDefaultConfiguration().encodeToStream(protocol,new Object[] {"putRecord",key,value});
                protocol.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        store.put(key,value);
        return this;
    }

    Thread _t;
    void checkThread() {
        if ( _t == null ) {
            _t = Thread.currentThread();
        } else if ( _t != Thread.currentThread() ){
            throw new RuntimeException("Unexpected MultiThreading");
        }
    }

    @Override
    public Record get(String key) {
        checkThread();
        return store.get(key);
    }

    @Override
    public Record remove(String key) {
        if ( protocol != null ) {
            try {
                FSTConfiguration.getDefaultConfiguration().encodeToStream(protocol,new Object[] {"remove",key});
                protocol.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Record v = get(key);
        if ( v != null ) {
            store.remove(key);
            v.internal_updateLastModified();
            if ( removeLog != null )
                removeLog.add(v.getLastModified(),v.getKey());
        }
        return v;
    }

    @Override
    public long size() {
        return store.getSize();
    }

    @Override
    public Stream<Record> stream() {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(store.values(), Spliterator.IMMUTABLE),
            false);
    }

    @Override
    public void resizeIfLoadFactorLarger(double loadFactor, long maxGrow) {
        double lf = (double)store.getUsedMem()/(double)(store.getUsedMem()+store.getFreeMem());
        if ( lf >= loadFactor ) {
            store.resizeStore(store.getCapacityMB()*1024l*1024l * 2,maxGrow);
        }
    }

    @Override
    public RemoveLog getRemoveLog() {
        return removeLog;
    }

    @Override
    public <T> void forEachWithSpore(Spore<Record, T> spore) {
        for (Iterator iterator = store.values(); iterator.hasNext(); ) {
            try {
                Record record = (Record) iterator.next();
                spore.remote(record);
            } catch ( Throwable ex ) {
                Log.Error(this, ex, "exception in spore " + ex);
//                throw ex; risky as prevents loading of tables in case
            }

            if ( spore.isFinished() )
                break;
        }
        spore.finish();
    }

    static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
    @Override
    public void _saveMapping(ClusterTableRecordMapping mapping) {
        if ( tableFile != null ) {
            String noExt = tableFile.substring( 0, tableFile.lastIndexOf(".") );
            try {
                Files.write(new File(noExt+".mapping" ).toPath(), conf.asByteArray(mapping));
            } catch (IOException e) {
                Log.Error(this,e);
            }
        }
    }

    @Override
    public ClusterTableRecordMapping _loadMapping() {
        if ( tableFile != null ) {
            String noExt = tableFile.substring( 0, tableFile.lastIndexOf(".") );
            try {
                File f = new File(noExt + ".mapping");
                if ( ! f.exists() )
                    return null;
                return (ClusterTableRecordMapping) conf.asObject(Files.readAllBytes(f.toPath()));
            } catch (IOException e) {
                Log.Error(this,e);
            }
        }
        return null;
    }

}
