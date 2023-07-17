package org.nustaq.kontraktor.services.datacluster.dynamic;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.KConnectionPool;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.base.ServiceDescription;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.services.*;
import org.nustaq.kontraktor.services.datacluster.DataCfg;
import org.nustaq.kontraktor.services.datacluster.DataShardArgs;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.api.RealLiveTable;
import org.nustaq.reallive.api.TableState;
import org.nustaq.reallive.server.actors.RealLiveTableActor;
import org.nustaq.reallive.server.storage.ClusterTableRecordMapping;
import org.nustaq.reallive.server.actors.DynTableSpaceActor;

import org.nustaq.reallive.api.Record;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DynDataShard extends ServiceActor<DynDataShard>  {

    public static final String DATA_SHARD_NAME = "DynShard";
    public static int WAIT_TABLE_LOAD = 60_000;

    DynTableSpaceActor tableSpace; // groups table actors
    KConnectionPool shardConnectionPool;

    @Override
    public IPromise<Void> init(ConnectableActor registryConnectable, ServiceArgs options, boolean auto /*ignored*/) {
        IPromise<Void> p = new Promise<>();
        shardConnectionPool = new KConnectionPool();
        try {
            super.init(registryConnectable, options,false).then( (r,e) -> {
                try {
                    config = serviceRegistry.get().getConfig().await();
                    if (!config.getDataCluster().isDynamic())
                    {
                        Log.Error(this,"dynamic nodes cannot run with nondynamic cluster configurations. set datacfg.isDynamic to true");
                        Thread.sleep(1000);
                        System.exit(1);
                    }
                    initTableSpace();
                    registerSelf();
                    p.resolve();
                } catch (Exception ex) {
                    Log.Error(this,ex);
                }
            });
        } catch (Throwable t) {
            p.reject(t);
        }
        return p;
    }

    protected int getPort() {
        if ( getCmdline().getDsPortOverride() > 0 )
            return getCmdline().getDsPortOverride();
        return cmdline.getDataShardPortBase()+getCmdline().getShardNo();
    }

    private int getShardNo() {
        return getCmdline().getShardNo();
    }

    protected void initTableSpace() {
        if ( config == null )
        {
            Log.Error(this,"no cluster config received or failed to connect Service Registry");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(1);
        }
        DataCfg dataCfg = config.getDataCluster();
        tableSpace = Actors.AsActor( DynTableSpaceActor.class );
        String dir = dataCfg.getDataDir()[0];
        new File(dir).mkdirs();
        tableSpace.setBaseDataDir(dir);
        tableSpace.init();
        Log.Info(this,"start loading tables");
        Arrays.stream(config.getDataCluster().getSchema())
            .forEach( td -> {
                try {
                    td.shardId(getServiceDescription().getName());
                    td.shardNo(getCmdline().getShardNo());
                    tableSpace.createOrLoadTable(td).await(WAIT_TABLE_LOAD);
                    Log.Info(this, "loaded table " + td.getName());
                } catch (Exception e) {
                    Log.Error(this,e, "failed to initialize table");
                }
            });
        Log.Info(this,"wait table space ping");
        tableSpace.ping().await();
        Log.Info(this, "finished init tablespace in "+
            dataCfg.getDataDir()[0]+
            " dynshard "+ getShardNo()
        );
    }

    public IPromise<DynTableSpaceActor> getTableSpace() {
        return resolve(tableSpace);
    }

    @Override
    protected boolean isFixedDataCluster() {
        return false;
    }

    @Override
    protected String[] getRequiredServiceNames() {
        return new String[0];
    }

    @Override
    protected ServiceDescription createServiceDescription() {
        return new ServiceDescription(DATA_SHARD_NAME + getShardNo())
            .connectable( new TCPConnectable(DynDataShard.class, cmdline.getHost(), getPort()));
    }

    protected DataShardArgs getCmdline() {
        return (DataShardArgs) cmdline;
    }

    @Override
    protected Serializable getStatus() {
        return "{ \"connections\":"+(__connections != null ? __connections.size() : 0)+"}";
    }

    public IPromise<Map<String, TableState>> getStates() {
        Promise p = new Promise();
        tableSpace.getStates().then( (r,e) -> {
            r.values().stream().forEach( ts -> ts.associatedShardName(serviceDescription.getName()));
            p.complete(r,e);
        });
        return p;
    }

    public IPromise _setMapping(String tableName, ClusterTableRecordMapping mapping) {
        return tableSpace._setMapping(tableName,mapping);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    //
    //  startup

    public static void main(String[] args) {
        start(args);
    }

    public static DynDataShard start(String[] args) {
        DataShardArgs options = (DataShardArgs) ServiceRegistry.parseCommandLine(args, null, DataShardArgs.New());
        return start(options);
    }

    public static DynDataShard start(DataShardArgs options) {
        DynDataShard ds = Actors.AsActor(DynDataShard.class,256000);

        TCPConnectable registryConnectable = new TCPConnectable(ServiceRegistry.class, options.getRegistryHost(), options.getRegistryPort());
        Log.Info(DynDataShard.class,"connect registry at "+registryConnectable);
        ds.init(registryConnectable, options, true).await();
        Log.Info(ds.getClass(), "Init finished");
        return ds;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // balancing related stuff

    public IPromise _moveHashShardsTo(String tableName, int[] hashShards2Move, ServiceDescription otherRef) {
        Promise res = new Promise();
        serialOn("RecordRedistribution#"+tableName, prom -> {
            shardConnectionPool.getConnection(otherRef.getConnectable()).then( (remote,err) -> {
                if ( remote != null ) {
                    ClusterTableRecordMapping movedMapping = new ClusterTableRecordMapping();
                    movedMapping.addBuckets(hashShards2Move);
                    try {
                        RealLiveTableActor table = (RealLiveTableActor) tableSpace.getTableAsync(tableName).await();
                        List<Record> toTransmit = new ArrayList<>();
                        table.forEach( record -> movedMapping.matches(record.getKey().hashCode()) , (r,e) -> {
                            if ( r != null )
                                toTransmit.add(r);
                            else {
                                Log.Info(this,"transmitting "+tableName+" "+toTransmit.size()+" records to "+otherRef.getName());
                                ((DynDataShard)remote)._receiveHashTransmission( tableName, hashShards2Move, toTransmit )
                                    .then( (rr,ee) -> {
                                        if ( ee == null ) {
                                            ClusterTableRecordMapping mapping = tableSpace.getMapping(tableName);
                                            ClusterTableRecordMapping newMapping = ClusterTableRecordMapping.Copy(mapping);
                                            newMapping.remove(hashShards2Move);

                                            toTransmit.forEach(rec->table._removeSilent(rec.getKey()));

                                            _setMapping(tableName,newMapping).then( (rrr,eee) -> {
                                                Log.Info(this,"COMPLETE: transmitting "+tableName+" "+toTransmit.size()+" records to "+otherRef.getName());
                                                prom.complete();
                                            });
                                            res.resolve();
                                        } else {
                                            res.reject(ee);
                                            prom.complete();
                                        }
                                    });
                            }
                        });
                    } catch (Exception e) {
                        res.reject(e);
                        prom.complete();
                    }
                } else {
                    prom.complete();
                    res.reject(err);
                }
            });
        });
        return res;
    }

    public IPromise _receiveHashTransmission(String tableName, int[] hashShards2Move, List<Record> toTransmit) {
        Promise p = new Promise();
        serialOn("RecordRedistribution#"+tableName, prom -> {
            // FIXME: suppress notifications
            Log.Info(this,"received transmision of "+toTransmit.size()+" records to "+tableName);
            RealLiveTableActor table = (RealLiveTableActor) tableSpace.getTableAsync(tableName).await();

            ClusterTableRecordMapping mapping = tableSpace.getMapping(tableName);
            ClusterTableRecordMapping newMapping = ClusterTableRecordMapping.Copy(mapping);
            newMapping.addBuckets(hashShards2Move);

            toTransmit.forEach( rec -> table._addSilent(rec) );

            _setMapping(tableName,newMapping).then( (r,e) -> prom.resolve() );
            p.complete();
        });
        return p;
    }
}
