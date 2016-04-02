package org.nustaq.reallive.impl.tablespace;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.reallive.impl.actors.RealLiveTableActor;
import org.nustaq.reallive.impl.storage.CachedOffHeapStorage;
import org.nustaq.reallive.impl.storage.HeapRecordStorage;
import org.nustaq.reallive.impl.storage.OffHeapRecordStorage;
import org.nustaq.reallive.interfaces.*;
import org.nustaq.reallive.messages.StateMessage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created by ruedi on 08.08.2015.
 */
public class TableSpaceActor extends Actor<TableSpaceActor> implements TableSpace {

    HashMap<String,RealLiveTable> tables;
    HashMap<String,TableDescription> tableDesc;
    List<Callback<StateMessage>> stateListeners;

    String baseDir;

    @Local
    public void init( int numScanThreads, int numFilterThreads ) {
        tables = new HashMap<>();
        stateListeners = new ArrayList<>();
        tableDesc = new HashMap<>();
    }

    /**
     * overrides setting in table description if set
     * @param dir
     */
    public void setBaseDataDir(String dir) {
        this.baseDir = dir;
    }

    protected String getBaseDir() {
        return baseDir;
    }

    @Override
    public IPromise<RealLiveTable> createOrLoadTable(TableDescription desc) {
        if ( tables.containsKey( desc.getName()) ) {
            return resolve(tables.get(desc.getName()));
        }
        RealLiveTableActor table = Actors.AsActor(RealLiveTableActor.class);

        Supplier<RecordStorage> memFactory;
        if ( desc.getFilePath() == null ) {
            switch( desc.getType() ) {
                case CACHED:
                    memFactory = () -> new CachedOffHeapStorage(
                        new OffHeapRecordStorage( desc.getKeyLen(), desc.getSizeMB(), desc.getNumEntries() ),
                        new HeapRecordStorage<>() );
                break;
                default:
                case PERSIST:
                    memFactory = () -> new OffHeapRecordStorage( desc.getKeyLen(), desc.getSizeMB(), desc.getNumEntries() );
                break;
                case TEMP:
                    memFactory = () -> new HeapRecordStorage<>();
                break;
            }
        } else {
            String bp = getBaseDir() == null ? desc.getFilePath() : getBaseDir();
            desc.filePath(bp);
            new File(bp).mkdirs();
            switch( desc.getType() ) {
                case CACHED:
                    memFactory = () -> new CachedOffHeapStorage(
                        new OffHeapRecordStorage(
                            bp+"/"+desc.getName()+"_"+desc.getShardNo()+".bin",
                            desc.getKeyLen(),
                            desc.getSizeMB(),
                            desc.getNumEntries()
                        ),
                        new HeapRecordStorage<>()
                    );
                break;
                default:
                case PERSIST:
                    memFactory = () ->
                        new OffHeapRecordStorage(
                            bp+"/"+desc.getName()+"_"+desc.getShardNo()+".bin",
                            desc.getKeyLen(),
                            desc.getSizeMB(),
                            desc.getNumEntries()
                        );
                break;
                case TEMP:
                    memFactory = () -> new HeapRecordStorage<>();
                break;
            }
        }
        table.init( memFactory, desc );
        tables.put(desc.getName(),table);
        return resolve(table);
    }

    @Override
    public IPromise dropTable(String name) {
        RealLiveTable realLiveTable = tables.get(name);
        if ( name != null ) {
            tables.remove(name);
            ((Actor)realLiveTable).stop();
        }
        return resolve();
    }

    @Override
    public IPromise<List<TableDescription>> getTableDescriptions() {
        return resolve( new ArrayList<>(tableDesc.values()) );
    }

    @Override
    public IPromise<List<RealLiveTable>> getTables() {
        return resolve( new ArrayList<>(tables.values()));
    }

    @Override
    public IPromise<RealLiveTable> getTable(String name) {
        return resolve(tables.get(name));
    }

    @Override
    public IPromise shutDown() {
        tables.values().forEach( table -> table.stop() );
        stream(((SimpleScheduler) getScheduler())).forEach(scheduler -> {
            scheduler.setKeepAlive(false);
            scheduler.terminateIfIdle();
        });
        return resolve();
    }

    @Override
    public void stateListener(Callback<StateMessage> stateListener) {
        this.stateListeners.add(stateListener);
    }

}
