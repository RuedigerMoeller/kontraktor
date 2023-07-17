package org.nustaq.reallive.client;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.Scheduler;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.api.RealLiveTable;
import org.nustaq.reallive.api.RecordStorage;
import org.nustaq.reallive.api.TableDescription;
import org.nustaq.reallive.server.actors.RealLiveTableActor;
import org.nustaq.reallive.server.storage.CachedOffHeapStorage;
import org.nustaq.reallive.server.storage.HeapRecordStorage;
import org.nustaq.reallive.server.storage.OffHeapRecordStorage;

import java.io.File;
import java.util.function.Function;

public class EmbeddedRealLive {

    private static EmbeddedRealLive instance = new EmbeddedRealLive();
    public static EmbeddedRealLive get() {
        return instance;
    }

    public IPromise<RealLiveTable> loadTable(String pathToBinFile) {
        TableDescription ts = new TableDescription()
            .storageType(TableDescription.PERSIST)
            .numEntries(100_000)
            .filePath(pathToBinFile)
            .alternativePath(pathToBinFile);
        return createTable(ts,null);
    }

    public IPromise<RealLiveTable> createTable(TableDescription desc, String dataDir) {
        return createTable(desc, dataDir,new SimpleScheduler());
    }

    /**
     * WARNING: never create more than one table using the same file. This will
     * result in corrupted data for sure. As actor refs (tables) are thread save,
     * just init a singleton containing all your tables once.
     *
     * @param desc
     * @param dataDir - if null use path from description
     * @return a thread save actor reference to a newly loaded or created table
     */
    public IPromise<RealLiveTable> createTable(TableDescription desc, String dataDir, Scheduler scheduler) {
        RealLiveTableActor table = Actors.AsActor(RealLiveTableActor.class,scheduler);

        Function<TableDescription,RecordStorage> memFactory;
        if (desc.getFilePath() == null) {
            Log.Info(this,"no file specified. all data in memory "+desc.getName());
            switch (desc.getStorageType()) {
                case TableDescription.CACHED:
                    memFactory = d -> new CachedOffHeapStorage(
                        new OffHeapRecordStorage(desc.getKeyLen(), desc.getSizeMB(), desc.getNumEntries()),
                        new HeapRecordStorage()
                    );
                    break;
                case TableDescription.PERSIST:
                    memFactory = d -> new OffHeapRecordStorage(desc.getKeyLen(), desc.getSizeMB(), desc.getNumEntries());
                    break;
                case TableDescription.TEMP:
                    memFactory = d -> new HeapRecordStorage();
                    break;
                default:
                    memFactory = d -> new HeapRecordStorage();
                    Log.Error(this,"unknown storage type "+desc.getStorageType()+" default to TEMP");
            }
        } else {
            String bp = dataDir == null ? desc.getFilePath() : dataDir;
            desc.filePath(bp);
            new File(bp).mkdirs();
            String file = desc.getStorageFile();
            switch (desc.getStorageType()) {
                case TableDescription.CACHED:
                    Log.Info(this,"memory mapping file "+file);
                    memFactory = d -> new CachedOffHeapStorage(
                        new OffHeapRecordStorage(
                            file,
                            desc.getKeyLen(),
                            desc.getSizeMB(),
                            desc.getNumEntries()
                        ),
                        new HeapRecordStorage()
                    );
                    break;
                case TableDescription.PERSIST:
                    Log.Info(this,"memory mapping file "+file);
                    memFactory = d ->
                        new OffHeapRecordStorage(
                            file,
                            desc.getKeyLen(),
                            desc.getSizeMB(),
                            desc.getNumEntries()
                        );
                    break;
                case TableDescription.TEMP:
                    memFactory = d -> new HeapRecordStorage();
                    break;
                default:
                    memFactory = d -> new HeapRecordStorage();
                    Log.Error(this,"unknown storage type "+desc.getStorageType()+" default to TEMP");
            }
        }
        Promise p = new Promise();
        table.init(memFactory, desc).then( (r,e) -> {
            if ( e == null )
                p.resolve(table);
            else
                p.reject(e);
        });
        return p;
    }

}
