package org.nustaq.reallive.server.actors;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.Pair;
import org.nustaq.kontraktor.util.PromiseLatch;
import org.nustaq.reallive.client.EmbeddedRealLive;
import org.nustaq.reallive.api.*;
import org.nustaq.reallive.client.ShardedTable;
import org.nustaq.reallive.messages.StateMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by ruedi on 08.08.2015.
 *
 * groups a set of table definitions. Runs server/node -side
 */
public class TableSpaceActor extends Actor<TableSpaceActor> implements TableSpace {

    public static long MAX_WAIT_MMAP = TimeUnit.MINUTES.toMillis(5);

    public transient String __clientsideTag; // might contain shard id on remote side (servicename)

    protected HashMap<String,RealLiveTableActor> tables;
    protected HashMap<String,TableDescription> tableDesc;
    protected List<Callback<StateMessage>> stateListeners;

    protected String baseDir;
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
        Promise res = new Promise();
        if ( desc.getSpreadOut() > 0 ) {
            PromiseLatch pl = new PromiseLatch(desc.getSpreadOut());
            List<Pair<RealLiveTable,TableDescription>> spreadChildren = new ArrayList<>();
            for ( int i = 0; i < desc.getSpreadOut(); i++ ) {
                TableDescription newDesc = desc.clone();
                newDesc.spreadIndex(i);
                newDesc.name(desc.getName()+"_sprd"+i);
                newDesc.spreadOut(0);
                createOrLoadTable(newDesc).then( (r,e) -> {
                    if ( e != null ) {
                        if ( e instanceof Exception )
                            Log.Error(this, (Exception) e );
                        else
                            Log.Error(this, ""+e );
                    } else {
                        spreadChildren.add(new Pair<>(r,newDesc));
                    }
                    pl.countDown();
                });
            }
            pl.getPromise().then( () -> {
                if ( spreadChildren.size() != desc.getSpreadOut() ) {
                    res.reject("could not load all spreads of "+desc);
                    return;
                }
                RealLiveTable newShards[] = new RealLiveTable[desc.getSpreadOut()];
                spreadChildren.sort( (a,b) -> Integer.compare(a.getSecond().getSpreadIndex(), b.getSecond().getSpreadIndex()) );
                spreadChildren.stream().map( p -> p.getFirst() ).collect(Collectors.toList()).toArray(newShards);
                res.resolve( new ShardedTable(newShards, desc));
            });
            return res;
        }
        if ( tables.containsKey( desc.getName()) ) {
            return resolve(tables.get(desc.getName()));
        }
        RealLiveTableActor table = createTableActor(desc);
        tables.put(desc.getName(),table);
        res.resolve(table);
        return res;
    }

    protected RealLiveTableActor createTableActor(TableDescription desc) {
        return (RealLiveTableActor) EmbeddedRealLive.get().createTable(desc,getBaseDir()).await(MAX_WAIT_MMAP);
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
