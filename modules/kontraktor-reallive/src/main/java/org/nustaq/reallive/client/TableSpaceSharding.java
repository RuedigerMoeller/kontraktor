package org.nustaq.reallive.client;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.server.actors.RealLiveTableActor;
import org.nustaq.reallive.server.actors.TableSpaceActor;
import org.nustaq.reallive.server.storage.StorageStats;
import org.nustaq.reallive.api.RealLiveTable;
import org.nustaq.reallive.api.TableDescription;
import org.nustaq.reallive.api.TableSpace;
import org.nustaq.reallive.messages.StateMessage;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by ruedi on 12.08.2015.
 * Provides a view on a clustered remote TableSpace as if it was a single node. Runs client side
 */
public class TableSpaceSharding implements TableSpace {

    protected List<TableSpaceActor> shards = new ArrayList<>();
    protected Map<String,RealLiveTable> tableMap = new Object2ObjectOpenHashMap<>();
    protected Map<String,TableDescription> tableDescriptionMap = new Object2ObjectOpenHashMap<>();

    public TableSpaceSharding(TableSpaceActor[] shards) {
        Arrays.stream(shards).forEach( sh -> addShard(sh));
    }

    private void addShard(TableSpaceActor sh) {
        if ( shards.indexOf(sh) < 0 )
            shards.add(sh);
        else
            Log.Warn(this,"double add of shard "+sh );
    }

    public IPromise init() {
        return new Promise("done");
    }

    @Override
    public IPromise<RealLiveTable> createOrLoadTable(TableDescription desc0) {
        Promise<RealLiveTable> res = new Promise();
        ArrayList<IPromise<RealLiveTable>> results = new ArrayList();
        for (int i = 0; i < shards.size(); i++) {
            TableSpaceActor shard = shards.get(i);
            TableDescription desc = desc0.clone().shardId(shard.__clientsideTag).shardNo(-1);
            IPromise<RealLiveTable> remoteTable = shard.createOrLoadTable(desc);
            Promise p = new Promise();
            results.add(p);
            final String finalId = shard.__clientsideTag;
            remoteTable.then((r, e) -> {
                if (e == null) {
                    Log.Debug(this, "table creation: " + desc.getName() + " " + finalId);
                    ((RealLiveTableActor) r).__clientSideTag = finalId;
                }
                else if ( e instanceof Throwable )
                    Log.Info(this, (Throwable) e,"failed table creation: " + desc.getName() + " "+ finalId );
                else
                    Log.Info(this, "failed table creation: " + desc.getName() + " "+ finalId +" "+e);
                p.complete(r, e);
            });
        }
        Log.Debug(this,"waiting for creation of tables ..");
        Actors.all(results).then( (List<IPromise<RealLiveTable>> tables,Object err) -> {
            Log.Debug(this,"table creation (waiting finished)");
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
                ShardedTable ts = createShardedTable(desc0, tableShards);
                tableMap.put(desc0.getName(),ts);
                tableDescriptionMap.put(desc0.getName(),desc0);
                res.resolve(ts);
            }
        });

        return res;
    }

    protected ShardedTable createShardedTable(TableDescription desc, RealLiveTable[] tableShards) {
        return new ShardedTable(tableShards, desc );
    }

    @Override
    public IPromise dropTable(String name) {
        ArrayList<IPromise<Object>> results = new ArrayList();
        for (int i = 0; i < shards.size(); i++) {
            TableSpaceActor shard = shards.get(i);
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
