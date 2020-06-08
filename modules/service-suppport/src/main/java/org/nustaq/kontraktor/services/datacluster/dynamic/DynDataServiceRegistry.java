package org.nustaq.kontraktor.services.datacluster.dynamic;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.services.ClusterCfg;
import org.nustaq.kontraktor.services.RegistryArgs;
import org.nustaq.kontraktor.remoting.base.ServiceDescription;
import org.nustaq.kontraktor.services.ServiceRegistry;
import org.nustaq.kontraktor.services.rlserver.SingleProcessRLClusterArgs;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.Pair;
import org.nustaq.reallive.api.TableState;
import org.nustaq.reallive.server.dynamic.DynClusterDistribution;
import org.nustaq.reallive.server.dynamic.DynClusterTableDistribution;
import org.nustaq.reallive.server.storage.ClusterTableRecordMapping;

import java.util.*;
import java.util.stream.Collectors;

import static org.nustaq.reallive.server.dynamic.DynClusterTableDistribution.*;

public class DynDataServiceRegistry extends ServiceRegistry {

    public static final String RECORD_DISTRIBUTION = "distribution";

    List<ServiceDescription> dynShards = new ArrayList<>();
    Map<String,DynDataShard> primaryDynShards = new HashMap<>();
    DynClusterDistribution activeDistribution;
    boolean autoStartUnderway = false;

    public void registerService(ServiceDescription desc ) {
        super.registerService(desc);
        if ( desc.getActorClazz() == DynDataShard.class ) {
            dynShards.add(desc);
            if ( ! autoStartUnderway && config.isDynAutoStart() ) {
                autoStartUnderway = true;
                delayed(1000, () -> waitForAutoStart() );
            }
        }
    }

    public IPromise<DynClusterDistribution> getActiveDistribution() {
        return resolve(activeDistribution);
    }

    protected void broadCastTimeOut(ServiceDescription desc) {
        dynShards.remove(desc);
        primaryDynShards.remove(desc.getName());
        super.broadCastTimeOut(desc);
    }

    public IPromise releaseDynShard(String shardName2Release) {
        Promise p = new Promise();
        collectRecordDistribution().then( (distribution,error) -> {
            if (error != null) {
                p.reject(error);
                return;
            }
            distribution.getTableNames()
                .forEach( tableName -> {
                    DynClusterTableDistribution tblDist = distribution.have(tableName);
                    List<TableState> states = tblDist.getStates();
                    TableState toRelease = null;
                    for (int i = 0; i < states.size(); i++) {
                        TableState tableState = states.get(i);
                        if ( shardName2Release.equals(tableState.getAssociatedShardName()) ) {
                            toRelease = tableState;
                            break;
                        }
                    }
                    int buckets = toRelease.getNumBuckets();
                    int bucketsPerNode = buckets/(states.size())+1;
                    for (int i = 0; i < states.size(); i++) {
                        TableState tableState = states.get(i);
                        if ( ! shardName2Release.equals(tableState.getAssociatedShardName()) ) {
                            int[] bucketsToRemove = toRelease.takeBuckets(bucketsPerNode);
                            if ( bucketsToRemove.length > 0 ) {
                                tblDist.addAction(new MoveHashShardsAction(
                                    bucketsToRemove,
                                    tableName,
                                    toRelease.getAssociatedShardName(),
                                    tableState.getAssociatedShardName()
                                ));
                            }
                            break;
                        }
                    }
                    // execute actions
                    List collect = distribution.getDistributions().entrySet().stream().map(en -> executeActions(en.getValue())).collect(Collectors.toList());
                    all(collect).then( (plist,finErr) -> {
                        Log.Info(this, "*****************************************************************************************************");
                        Log.Info(this, "table release processed ");
                        if ( finErr != null ) {
                            Log.Error(this, "  with ERROR:" + finErr);
                            p.reject(finErr);
                        }
                        else {
                            distribution.clearActions();
                            publishDistribution(distribution);
                            p.resolve();
                        }
                        Log.Info(this, "*****************************************************************************************************");
                    });
                });
        });
        return p;
    }

    public IPromise balanceDynShards() {
        Promise p = new Promise();
        // get full cluster distribution state
        // fill DynClusterTableDistribution contained in DynClusterDistribution with Actions
        // process those actions afterwards
        collectRecordDistribution().then( (distribution,error) -> {
            if ( error != null ) {
                p.reject(error);
                return;
            }

            distribution.getTableNames()
                .forEach( tableName -> computeDistributionActions(distribution.have(tableName)));

            // execute actions
            List collect = distribution.getDistributions().entrySet().stream().map(en -> executeActions(en.getValue())).collect(Collectors.toList());
            all(collect).then( (plist,finErr) -> {
                Log.Info(this, "*****************************************************************************************************");
                Log.Info(this, "all table distributions processed ");
                if ( finErr != null ) {
                    Log.Error(this, "  with ERROR:" + finErr);
                    p.reject(finErr);
                }
                else {
                    distribution.clearActions();
                    publishDistribution(distribution);
                    p.resolve();
                }
                Log.Info(this, "*****************************************************************************************************");
            });
        });
        return p;
    }

    /**
     * collects distribution and triggers a balance as soon full coverage is present
     */
    protected void waitForAutoStart() {
        collectRecordDistribution().then( (dist,err) -> {
            if ( dist != null && dist.hasFullCoverage() ) {
                Log.Info(this,"**** auto start dyn cluster ****");
                execute( () -> balanceDynShards() );
            } else {
                Log.Info(this,"autostarter waiting for hash coverage ... ");
                delayed(2000, () -> waitForAutoStart() );
            }
        });
    }

    // get full cluster distribution state
    protected IPromise<DynClusterDistribution> collectRecordDistribution( ) {
        Promise res = new Promise();
        all(
            dynShards.stream()
                .map(desc->getOrConnect(desc.getName()))
                .collect(Collectors.toList())
        )
        .then( (listofproms,err) -> {

            List<DynDataShard> activeShards =
                listofproms.stream()
                    .map(x -> x.get()).collect(Collectors.toList());

            List<IPromise<Map<String, TableState>>> tableStateProms =
                activeShards.stream()
                    .map(shard -> shard.getStates())
                    .collect(Collectors.toList());

            all(tableStateProms).then( (r,e) -> {
                if ( e != null )
                    res.reject(e);

                List<Map<String, TableState>> tableStates =
                    tableStateProms.stream()
                        .map(x -> x.get())
                        .collect(Collectors.toList());

                DynClusterDistribution distribution = new DynClusterDistribution();
                tableStates.stream()
                    .flatMap( map -> map.entrySet().stream() )
                    .forEach( en -> distribution.have(en.getKey()).add(en.getValue()) );

                res.resolve(distribution);
            });
        });
        return res;
    }

    private void publishDistribution(DynClusterDistribution distribution) {
        this.activeDistribution = distribution;
        broadcastDistribution(distribution);
    }

    protected void broadcastDistribution(DynClusterDistribution mapping) {
        Pair msg = new Pair(RECORD_DISTRIBUTION,mapping);
        listeners = listeners.stream().filter( cb -> !cb.isTerminated()).collect(Collectors.toList());
        listeners.forEach(cb -> {
            try {
                cb.pipe(msg);
            } catch (Throwable th) {
                Log.Info(this, th);
            }
        });
    }

    private IPromise executeActions(DynClusterTableDistribution tdist) {
        if ( tdist.getActions().size() == 0 )
            return resolve();
        Promise p = new Promise();
        List pendingActions = new ArrayList<>();
        tdist.getActions().forEach(action -> {
            IPromise<DynDataShard> primaryShard = getOrConnect(action.getShardName());
            ServiceDescription other = null;
            if ( action.getOtherShard() != null )
                other = getService(action.getOtherShard());
            System.out.println(action);
            ServiceDescription finalOther = other;
            primaryShard.then( (shard, error) -> {
                if ( error != null )
                    Log.Error(this, ""+error );
                else
                    pendingActions.add(action.action(shard, finalOther));
                if ( pendingActions.size() == tdist.getActions().size() ) {
                    all(pendingActions).then((promlist,paError) -> {
                        if ( paError == null ) {
                            Log.Info(this,"actions for table "+tdist.getName()+" done.");
                            p.resolve(true);
                        }
                        else {
                            Log.Error(this,"actions for table "+tdist.getName()+" FAILED."+paError);
                            p.reject(paError);
                        }
                    });
                }
            });
        });
        return p;
    }

    private void computeDistributionActions(DynClusterTableDistribution distribution) {
        int sanRes = distribution.sanitize();
        Log.Info(this,"sanitize distribution "+distribution.getName()+" result:"+sanRes);
        switch ( sanRes ) {
            case OK:
                break;
            case EMPTY:
            case EMPTY|INCOMPLETE:
                initFromEmpty(distribution);
                break;
            case INCOMPLETE:
                Log.Warn(this,"incomplete distribution detected in "+distribution.getName()+", either caused by sparse records or a datanode is missing");
                initFromIncomplete(distribution);
                int debug = distribution.sanitize();
                if ( debug != 0 ) {
                    Log.Error(this,"distribution still invalid after sanitation "+distribution);
                }
                break;
            case INTERSECT:
            default:
                System.out.println(distribution);
                throw new RuntimeException("unhandled cluster distribution state "+distribution.getName());
        }
        //check balance
        int sum = 0;
        List<TableState> states = distribution.getStates();
        for (int i = 0; i < states.size(); i++) {
            TableState tableState = states.get(i);
            sum += tableState.getMapping().getBitset().cardinality();
        }
        int avg = sum / states.size();
        List<TableState> receiver = new ArrayList<>();
        List<TableState> sender = new ArrayList<>();
        for (int i = 0; i < states.size(); i++) {
            TableState tableState = states.get(i);
            int diff = tableState.getNumBuckets()-avg;
            if ( diff >= 2 )
                sender.add(tableState);
            else if ( diff <= -2 )
                receiver.add(tableState);
        }
        sender.sort( (a,b) -> a.getNumBuckets() - b.getNumBuckets() );
        receiver.sort( (b,a) -> a.getNumBuckets() - b.getNumBuckets() );
        while ( sender.size() > 0 && receiver.size() > 0 ) {
            TableState sendTS = sender.get(0);
            TableState recTS = receiver.get(0);
            int diffSender = sendTS.getNumBuckets() - avg;
            int diffReceiver = recTS.getNumBuckets() - avg;
            int transfer = Math.min( -diffReceiver, diffSender );
            int transferHashes[] = sendTS.takeBuckets(transfer);
            recTS.addBuckets(transferHashes);
            for ( int i = 0; i < transfer; i++ ) {
                distribution.addAction(
                    new MoveHashShardsAction(
                        transferHashes,
                        sendTS.getTableName(),
                        sendTS.getAssociatedShardName(),
                        recTS.getAssociatedShardName()
                    )
                );
            }
            if ( sendTS.getNumBuckets()-avg > avg-recTS.getNumBuckets() )
                receiver.remove(0);
            else
                sender.remove(0);
        }

    }

    // had to be moved here as requires a lot of refs to kontraktor kluster
    void initFromIncomplete(DynClusterTableDistribution distribution) {
        List<TableState> tableStates = distribution.getStates();
        int numNodes = tableStates.size();
        int tsCount = 0;
        for (int i = 0; i < ClusterTableRecordMapping.NUM_BUCKET; i++ ) {
            TableState tableState = tableStates.get(tsCount++);
            if ( ! distribution.covers(i) ) {
                tableState.getMapping().setBucket(i,true);
            }
            if ( tsCount >= numNodes )
                tsCount = 0;
        }
        distribution.setActions(new ArrayList<>());
        tableStates.forEach( tstate -> distribution.addAction(
            new AssignMappingAction(
                tstate.getTableName(),
                tstate.getAssociatedShardName(),
                tstate.getMapping()
            )
        ));
    }

    // had to be moved here as requires a lot of refs to kontraktor kluster
    void initFromEmpty(DynClusterTableDistribution distribution) {
        List<TableState> tableStates = distribution.getStates();
        int numNodes = tableStates.size();
        int tsCount = 0;
        for (int i = 0; i < ClusterTableRecordMapping.NUM_BUCKET; i++ ) {
            TableState tableState = tableStates.get(tsCount++);
            tableState.getMapping().setBucket(i,true);
            if ( tsCount >= numNodes )
                tsCount = 0;
        }
        distribution.setActions(new ArrayList<>());
        tableStates.forEach( tstate -> distribution.addAction(
            new AssignMappingAction(
                tstate.getTableName(),
                tstate.getAssociatedShardName(),
                tstate.getMapping()
            )
        ));
    }

    protected IPromise<DynDataShard> getOrConnect(String name) {
        if ( primaryDynShards.get(name) != null ) {
            return resolve(primaryDynShards.get(name));
        }
        if ( primaryDynShards.get(name) == null ) {
            for (int i = 0; i < dynShards.size(); i++) {
                ServiceDescription serviceDescription = dynShards.get(i);
                if ( serviceDescription.getName().equals(name) ) {
                    Promise p = new Promise();
                    serviceDescription.getConnectable().connect((r,e) -> { /*ignored*/}, actor -> {
                       Log.Error(this,"unhandled disconnect "+actor);
                    }).then( (r,e) -> {
                        primaryDynShards.put(name, (DynDataShard) r);
                        p.complete(r,e);
                    });
                    return p;
                }
            }
        }
        return resolve(null);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        start(args);
    }

    public static ServiceRegistry start(String[] args) {
        options = (RegistryArgs) parseCommandLine(args,null,RegistryArgs.New());
        return start(options);
    }

    public static ServiceRegistry start(RegistryArgs options) {
        return start(options,null, DynDataServiceRegistry.class);
    }

    public static void start(SingleProcessRLClusterArgs options, ClusterCfg cfg) {
        start(options,cfg,DynDataServiceRegistry.class);
    }

}
