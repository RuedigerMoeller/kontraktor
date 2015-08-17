package org.nustaq.reallive.impl.tablespace;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.reallive.impl.actors.RealLiveStreamActor;
import org.nustaq.reallive.impl.storage.OffHeapRecordStorage;
import org.nustaq.reallive.interfaces.*;
import org.nustaq.reallive.messages.StateMessage;
import org.nustaq.reallive.records.MapRecord;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by ruedi on 08.08.2015.
 */
public class TableSpaceActor extends Actor<TableSpaceActor> implements TableSpace {

    HashMap<String,RealLiveTable> tables;
    HashMap<String,TableDescription> tableDesc;
    List<Callback<StateMessage>> stateListeners;

    SimpleScheduler scanScheduler[];
    SimpleScheduler filterScheduler[];
    int scanQSize = 64000;
    int filterQSize = 64000;
    String baseDir;

    @Local
    public void init( int numScanThreads, int numFilterThreads ) {
        initWitAllOptions(numScanThreads,numFilterThreads,scanQSize,filterQSize);
    }

    @Local
    public void initWitAllOptions( int numScanThreads, int numFilterThreads, int scanQSize, int filterQSize ) {
        tables = new HashMap<>();
        stateListeners = new ArrayList<>();
        tableDesc = new HashMap<>();
        scanScheduler = new SimpleScheduler[numScanThreads];
        filterScheduler = new SimpleScheduler[numFilterThreads];
        for (int i = 0; i < scanScheduler.length; i++) {
            scanScheduler[i] = new SimpleScheduler(scanQSize,true);
        }
        for (int i = 0; i < filterScheduler.length; i++) {
            filterScheduler[i] = new SimpleScheduler(filterQSize,true);
        }
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
        if ( tables.containsKey( desc.getName()) )
            reject("Table exists");
        RealLiveStreamActor table = Actors.AsActor(RealLiveStreamActor.class, loadBalance(desc));

        Supplier<RecordStorage> memFactory;
        if ( desc.getFilePath() == null ) {
            memFactory = () -> new OffHeapRecordStorage( 48, desc.getSizeMB(), desc.getNumEntries() );
        } else {
            new File(desc.getFilePath()).mkdirs();
            String bp = getBaseDir() == null ? desc.getFilePath() : getBaseDir();
            desc.filePath(bp);
            new File(bp).mkdirs();
            memFactory = () ->
                new OffHeapRecordStorage(
                    bp+"/"+desc.getName()+"_"+desc.getShardNo()+".bin",
                    48,
                    desc.getSizeMB(),
                    desc.getNumEntries()
                );
        }
        table.init( memFactory, loadBalanceFilter(desc), desc );
        tables.put(desc.getName(),table);
        return resolve(table);
    }

    int roundRobinFilterCounter = 0;
    private Scheduler loadBalanceFilter(TableDescription desc) {
        if ( filterScheduler.length <= 0 )  // if no threads, just use a scan thread
            return loadBalance(desc);
        roundRobinFilterCounter++;
        if ( roundRobinFilterCounter >= filterScheduler.length )
            roundRobinFilterCounter = 0;
        return filterScheduler[roundRobinFilterCounter];
    }

    int roundRobinScanCounter = 0;
    private Scheduler loadBalance(TableDescription desc) {
        roundRobinScanCounter++;
        if ( roundRobinScanCounter >= scanScheduler.length )
            roundRobinScanCounter = 0;
        return scanScheduler[roundRobinScanCounter];
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
        Consumer<SimpleScheduler> loop = scheduler -> {
            scheduler.setKeepAlive(false);
            scheduler.terminateIfIdle();
        };
        stream(filterScheduler).forEach(loop);
        stream(scanScheduler).forEach(loop);
        return resolve();
    }

    @Override
    public void stateListener(Callback<StateMessage> stateListener) {
        this.stateListeners.add(stateListener);
    }

}
