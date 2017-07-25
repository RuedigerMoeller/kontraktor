package org.nustaq.reallive.impl.tablespace;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.PromiseLatch;
import org.nustaq.reallive.impl.actors.ShardFunc;
import org.nustaq.reallive.impl.actors.TableSharding;
import org.nustaq.reallive.impl.storage.StorageStats;
import org.nustaq.reallive.interfaces.RealLiveTable;
import org.nustaq.reallive.interfaces.TableDescription;
import org.nustaq.reallive.interfaces.TableSpace;
import org.nustaq.reallive.messages.StateMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ruedi on 12.08.2015.
 */
public class TableSpaceSharding implements TableSpace {

    TableSpaceActor shards[];
    HashMap<String,RealLiveTable> tableMap = new HashMap();
    HashMap<String,TableDescription> tableDescriptionMap = new HashMap();
    ShardFunc func;

    public TableSpaceSharding(TableSpaceActor[] shards, ShardFunc func) {
        this.shards = shards;
        this.func = func;
    }

    public IPromise init() {
        return new Promise("done");
    }

    @Override
    public IPromise<RealLiveTable> createOrLoadTable(TableDescription desc) {
        Promise<RealLiveTable> res = new Promise();
        ArrayList<IPromise<RealLiveTable>> results = new ArrayList();
        for (int i = 0; i < shards.length; i++) {
            TableSpaceActor shard = shards[i];
            IPromise<RealLiveTable> table = shard.createOrLoadTable(desc.clone().shardNo(i));
            Promise p = new Promise();
            results.add(p);
            final int finalI = i;
            table.then((r, e) -> {
                if (e == null)
                    Log.Info(this, "table creation: "+desc.getName()+" "+ finalI);
                else
                    Log.Info(this, "failed table creation: " + desc.getName() + " "+ finalI + " " + e);
                p.complete(r, e);
            });
        }
        List<IPromise<RealLiveTable>> tables = Actors.all(results).await();

        RealLiveTable tableShards[] = new RealLiveTable[tables.size()];
        boolean errors = false;
        for (int i = 0; i < tables.size(); i++) {
            if ( tables.get(i).get() == null ) {
                res.reject(tables.get(i).getError());
                errors = true;
                break;
            } else {
                int sno = i;//tables[i].get().shardNo();
                if ( tableShards[sno] != null ) {
                    res.reject("shard "+sno+" is present more than once");
                    errors = true;
                    break;
                }
                tableShards[sno] = tables.get(i).get();
            }
        }
        if ( ! errors ) {
            TableSharding ts = new TableSharding(func, tableShards, desc );
            tableMap.put(desc.getName(),ts);
            tableDescriptionMap.put(desc.getName(),desc);
            res.resolve(ts);
        }
        return res;
    }

    @Override
    public IPromise dropTable(String name) {
        ArrayList<IPromise<Object>> results = new ArrayList();
        for (int i = 0; i < shards.length; i++) {
            TableSpaceActor shard = shards[i];
            results.add(shard.dropTable(name));
        }
        return Actors.all(results);
    }

    @Override
    public IPromise<List<TableDescription>> getTableDescriptions() {
        List<IPromise> collect = tableMap.values().stream().map(ts -> ts.getDescription()).collect(Collectors.toList());
        return Actors.allMapped((List)collect);
    }

    public List<StorageStats> getStats() {
        return tableMap.keySet().stream()
            .map(tableName -> ((StorageStats) tableMap.get(tableName).getStats().await()).tableName(tableName))
            .collect(Collectors.toList());
    }

    @Override
    public IPromise<List<RealLiveTable>> getTables() {
        return new Promise(new ArrayList(tableMap.values()));
    }

    @Override
    public IPromise<RealLiveTable> getTableAsync(String name) {
        return Actors.resolve(tableMap.get(name));
    }

    @Override
    public IPromise shutDown() {
        return new Promise("void");
    }

    @Override
    public void stateListener(Callback<StateMessage> stateListener) {
        throw new RuntimeException("unimplemented");
    }
}
