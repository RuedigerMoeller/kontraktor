package org.nustaq.reallive.impl.tablespace;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.reallive.impl.actors.ShardFunc;
import org.nustaq.reallive.impl.actors.TableSharding;
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
    HashMap<String,RealLiveTable> tableMap = new HashMap<>();
    HashMap<String,TableDescription> tableDescriptionMap = new HashMap<>();
    ShardFunc func;

    public TableSpaceSharding(TableSpaceActor[] shards, ShardFunc func) {
        this.shards = shards;
        this.func = func;
        List<TableDescription> tds = shards[0].getTableDescriptions().await();
        tds.forEach( tableDesc -> {
            tableDescriptionMap.put(tableDesc.getName(),tableDesc);
            tableMap.put(tableDesc.getName(), shards[0].getTable(tableDesc.getName()).await());
        });
    }

    public void init(TableSpaceActor shards[], ShardFunc func) {
        this.shards = shards;
        this.func = func;
        tableMap = new HashMap<>();
    }

    @Override
    public IPromise<RealLiveTable> createTable(TableDescription desc) {
        Promise<RealLiveTable> res = new Promise<>();
        ArrayList<IPromise> results = new ArrayList<>();
        for (int i = 0; i < shards.length; i++) {
            TableSpaceActor shard = shards[i];
            results.add(shard.createTable(desc));
        }
        IPromise<RealLiveTable>[] tables = Actors.all(res).await();
        RealLiveTable tableShards[] = new RealLiveTable[tables.length];
        boolean errors = false;
        for (int i = 0; i < tables.length; i++) {
            if ( tables[i].get() == null ) {
                res.reject(tables[i].getError());
                errors = true;
                break;
            } else {
                int sno = i;//tables[i].get().shardNo();
                if ( tableShards[sno] != null ) {
                    res.reject("shard "+sno+" is present more than once");
                    errors = true;
                    break;
                }
                tableShards[sno] = tables[i].get();
            }
        }
        if ( ! errors ) {
            TableSharding ts = new TableSharding(func, tableShards, desc );
            tableMap.put(desc.getName(),ts);
        }
        return res;
    }

    @Override
    public IPromise dropTable(String name) {
        ArrayList<IPromise<Object>> results = new ArrayList<>();
        for (int i = 0; i < shards.length; i++) {
            TableSpaceActor shard = shards[i];
            results.add(shard.dropTable(name));
        }
        return Actors.all(results);
    }

    @Override
    public IPromise<List<TableDescription>> getTableDescriptions() {
        return new Promise(tableMap.values().stream().map( ts -> ts.getDescription() ).collect(Collectors.toList()));
    }

    @Override
    public IPromise<List<RealLiveTable>> getTables() {
        return new Promise<>(new ArrayList(tableMap.values()));
    }

    @Override
    public IPromise<RealLiveTable> getTable(String name) {
        return Actors.resolve(tableMap.get(name));
    }

    @Override
    public IPromise shutDown() {
        return new Promise<>("void");
    }

    @Override
    public void stateListener(Callback<StateMessage> stateListener) {
        throw new RuntimeException("unimplemented");
    }
}
