package org.newstaq.kontraktor.services.rlclient;

import org.newstaq.kontraktor.services.ServiceActor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.impl.tablespace.ClusteredTableSpaceClient;
import org.nustaq.reallive.impl.tablespace.TableSpaceActor;
import org.nustaq.reallive.impl.tablespace.TableSpaceSharding;
import org.nustaq.reallive.interfaces.RLPredicate;
import org.nustaq.reallive.interfaces.RealLiveTable;
import org.nustaq.reallive.interfaces.Record;
import org.nustaq.reallive.interfaces.TableDescription;
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
    HashMap syncTableAccess;

    public IPromise connect( DataCfg config, TableSpaceActor shards[], ServiceActor hostingService ) {
        this.config = config;
        this.hostingService=hostingService;
        this.shards = shards;
        syncTableAccess = new HashMap();
        tableSharding = new TableSpaceSharding(shards,key -> Math.abs(key.hashCode())%shards.length);
        tableSharding.init().await();
        TableDescription[] schema = config.getSchema();
        return all( schema.length, i -> {
            Promise p = new Promise();
            tableSharding.createOrLoadTable(schema[i]).then( (r,e) -> {
                if ( r != null ) {
                    syncTableAccess.put(schema[i].getName(), r);
                }
                p.complete(r,e);
            });
            return p;
        });
    }

    @CallerSideMethod
    public RealLiveTable<String> getTableSync( String name ) {
        return (RealLiveTable<String>) getActor().syncTableAccess.get(name);
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
                            RealLiveTable table = shard.getTable(desc.getName()).await(60_000);
                            table.filter( rec -> true, (rec,err) -> {
                                if ( rec != null ) {
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

    public IPromise<Integer> getNoShards() {
        return resolve(shards.length);
    }

    public void processSharded( String tableName, RLPredicate<Record<String>> predicate, int shardNo, Callback<Record> cb  ) {
        TableSpaceActor shard = shards[shardNo];
        shard.getTable(tableName).then( t -> {
            RealLiveTable table = t;
            table.filter(predicate, cb);
        });
    }

}
