package org.nustaq.reallive.impl.tablespace;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.reallive.client.EmbeddedRealLive;
import org.nustaq.reallive.impl.actors.RealLiveTableActor;
import org.nustaq.reallive.impl.storage.CachedOffHeapStorage;
import org.nustaq.reallive.impl.storage.HeapRecordStorage;
import org.nustaq.reallive.impl.storage.OffHeapRecordStorage;
import org.nustaq.reallive.api.*;
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
    public void init() {
        tables = new HashMap();
        stateListeners = new ArrayList();
        tableDesc = new HashMap();
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
        RealLiveTableActor table = createTableActor(desc);
        tables.put(desc.getName(),table);
        return resolve(table);
    }

    private RealLiveTableActor createTableActor(TableDescription desc) {
        return (RealLiveTableActor) EmbeddedRealLive.get().createTable(desc,getBaseDir());
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
        return resolve( new ArrayList(tableDesc.values()) );
    }

    @Override
    public IPromise<List<RealLiveTable>> getTables() {
        return resolve( new ArrayList(tables.values()));
    }

    @Override
    public IPromise<RealLiveTable> getTableAsync(String name) {
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
