package org.nustaq.kontraktor.services.rlclient;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.services.datacluster.DataCfg;
import org.nustaq.kontraktor.services.ServiceActor;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.services.datacluster.dynamic.DynClusterDistribution;
import org.nustaq.kontraktor.services.datacluster.dynamic.DynDataServiceRegistry;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.client.ShardedTable;
import org.nustaq.reallive.client.ClusteredTableSpaceClient;
import org.nustaq.reallive.server.actors.TableSpaceActor;
import org.nustaq.reallive.client.TableSpaceSharding;
import org.nustaq.reallive.api.RLPredicate;
import org.nustaq.reallive.api.RealLiveTable;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.api.TableDescription;
import org.nustaq.serialization.FSTConfiguration;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by ruedi on 15.08.2015.
 *
 * connector actor to rl data cluster
 *
 */
public class DataClient<T extends DataClient> extends ClusteredTableSpaceClient<T> {

    DataCfg config;
    ServiceActor hostingService;
    TableSpaceActor shards[];
    HashMap<String,RealLiveTable> syncTableAccess;
    DynClusterDistribution currentMapping; // only used in dyn clusters

    public IPromise connect( DataCfg config, TableSpaceActor shards[], ServiceActor hostingService ) {
        this.config = config;
        this.hostingService=hostingService;
        this.shards = shards;
        syncTableAccess = new HashMap();
        tableSpaceSharding = createTableSpaceSharding(shards);
        tableSpaceSharding.init().await();
        if ( hostingService.getServiceRegistry() instanceof DynDataServiceRegistry ) {
            hostingService.addServiceEventListener((event, arg) -> handleServiceEvent((String) event, arg));
            currentMapping = ((DynDataServiceRegistry) hostingService.getServiceRegistry()).getActiveDistribution().await();
        }
        TableDescription[] schema = config.getSchema();
        return all( schema.length, i -> {
            TableDescription desc = schema[i];
            return initTable(desc);
        });
    }

    protected void handleServiceEvent(String event, Object arg) {
        if ( event.equals(DynDataServiceRegistry.RECORD_DISTRIBUTION) ) {
            currentMapping = (DynClusterDistribution) arg;
        }
    }

    protected TableSpaceSharding createTableSpaceSharding(TableSpaceActor[] shards) {
        return new TableSpaceSharding(shards);
    }

    private IPromise<Object> initTable(TableDescription desc) {
        Promise p = new Promise();
        tableSpaceSharding.createOrLoadTable(desc).then( (r, e) -> {
            if ( r != null ) {
                syncTableAccess.put(desc.getName(), r);
            }
            p.complete(r,e);
        });
        return p;
    }

    @CallerSideMethod
    public RealLiveTable getTable(String name ) {
        return (RealLiveTable) getActor().syncTableAccess.get(name);
    }

    /**
     * shorthand for getTable
     * @param name
     * @return
     */
    @CallerSideMethod
    public RealLiveTable tbl(String name ) {
        return (RealLiveTable) getActor().syncTableAccess.get(name);
    }

    public IPromise<Integer> getNoShards() {
        return resolve(shards.length);
    }

    public void nodeDisconnected(Actor act) {
        syncTableAccess.values().forEach( table -> ((ShardedTable)table).removeNode(act.getActorRef()));
    }

    @CallerSideMethod
    public TableSpaceActor[] getShards() {
        return getActor().shards;
    }

    public void unsubscribe(int subsId) {
        syncTableAccess.values().forEach( table -> table.unsubscribeById(subsId));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // utils for shardwise special processing
    //

    public void processSharded(String tableName, RLPredicate<Record> predicate, int shardNo, Callback<Record> cb  ) {
        TableSpaceActor shard = shards[shardNo];
        shard.getTableAsync(tableName).then(t -> {
            RealLiveTable table = t;
            table.forEach(predicate, cb);
        });
    }

    /**
     * @param directory
     */
    public IPromise export(String directory) {
        Promise res = new Promise();
        // use separate thread to enable slowly, blocking processing
        Actors.exec.execute( () -> {
            File d = new File(directory);
            if ( d.exists() && (! d.isDirectory() || ! d.canWrite()) ) {
                res.reject(new RuntimeException("cannot write to "+d+" or not a directory"));
                return;
            } else {
                d.mkdirs();
            }
            FSTConfiguration writeConf = FSTConfiguration.createDefaultConfiguration();
            Arrays.stream(config.getSchema()).forEach( desc -> {
                try {
                    DataOutputStream fout = new DataOutputStream(new FileOutputStream(new File(d,desc.getName()+".oos")));
                    CountDownLatch pl = new CountDownLatch(shards.length);
                    for (int i = 0; i < shards.length; i++) {
                        TableSpaceActor shard = shards[i];
                        Log.Info( this, "exporting shard "+i+" table "+desc.getName() );
                        try {
                            RealLiveTable table = shard.getTableAsync(desc.getName()).await(60_000);
                            table.forEach( rec -> true, (rec,err) -> {
                                if ( rec != null ) {
                                    writeRecord(writeConf, fout, rec);
                                } else if (err != null ) {
                                    Log.Warn(this,"error during export "+err);
                                    pl.countDown();
                                } else { // fin
                                    pl.countDown();
                                }
                            });
                        } catch (Exception e) {
                            Log.Error(this,"export failure "+desc.getName()+" shard "+i);
                        }
                    }
                    try {
                        boolean succ = pl.await(5, TimeUnit.MINUTES);
                        if ( ! succ )
                            Log.Error(this,"export timed out on table "+desc.getName());
                        try {
                            fout.close();
                        } catch (IOException e) {
                            Log.Error(this, e);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (FileNotFoundException e) {
                    Log.Error(this,e);
                }
            });
            res.complete();
        });
        return res;
    }

    private void writeRecord(FSTConfiguration writeConf, DataOutputStream fout, Record rec) {
        try {
            // write marker to enable recovery in case of corruption
            synchronized (fout) {
                fout.write(31);
                fout.write(32);
                fout.write(33);
                fout.write(34);
                byte[] b = writeConf.asByteArray(rec);
                fout.writeInt(b.length);
                fout.write(b);
            }
        } catch (IOException e) {
            Log.Error(this,e);
        }
    }
}
