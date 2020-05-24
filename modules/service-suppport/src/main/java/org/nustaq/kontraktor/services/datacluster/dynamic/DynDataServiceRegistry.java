package org.nustaq.kontraktor.services.datacluster.dynamic;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.services.ClusterCfg;
import org.nustaq.kontraktor.services.RegistryArgs;
import org.nustaq.kontraktor.services.ServiceDescription;
import org.nustaq.kontraktor.services.ServiceRegistry;
import org.nustaq.kontraktor.services.rlserver.SingleProcessRLClusterArgs;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.Pair;
import org.nustaq.reallive.api.TableState;

import java.util.*;
import java.util.stream.Collectors;

import static org.nustaq.kontraktor.services.datacluster.dynamic.DynClusterTableDistribution.*;

public class DynDataServiceRegistry extends ServiceRegistry {

    public static final String RECORD_DISTRIBUTION = "distribution";

    List<ServiceDescription> dynShards = new ArrayList<>();
    Map<String,DynDataShard> primaryDynShards = new HashMap<>();
    DynClusterDistribution activeDistribution;

    public void registerService(ServiceDescription desc ) {
        super.registerService(desc);
        if ( desc.getActorClazz() == DynDataShard.class ) {
            dynShards.add(desc);
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

    public IPromise balanceDynShards() {
        // get full cluster distribution state
        // fill DynClusterTableDistribution contained in DynClusterDistribution with Actions
        // process those actions afterwards
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
                List<Map<String, TableState>> tableStates =
                    tableStateProms.stream()
                        .map(x -> x.get())
                        .collect(Collectors.toList());

                DynClusterDistribution distribution = new DynClusterDistribution();
                tableStates.stream()
                    .flatMap( map -> map.entrySet().stream() )
                    .forEach( en -> distribution.have(en.getKey()).add(en.getValue()) );

                distribution.getTableNames()
                    .forEach( tableName -> computeDistributionActions(distribution.have(tableName)));

                List collect = distribution.distributions.entrySet().stream().map(en -> executeActions(en.getValue())).collect(Collectors.toList());
                all(collect).then( (plist,finErr) -> {
                    Log.Info(this, "*****************************************************************************************************");
                    Log.Info(this, "all table distributions processed ");
                    if ( finErr != null )
                        Log.Error(this, "  with ERROR:"+finErr);
                    else {
                        distribution.clearActions();
                        publishDistribution(distribution);
                    }
                    Log.Info(this, "*****************************************************************************************************");
                });
            });
        });
        return resolve(null);
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
        Promise p = new Promise();
        List pendingActions = new ArrayList<>();
        System.out.println(tdist);
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
                distribution.initFromEmpty();
                break;
            case INTERSECT:
            default:
                throw new RuntimeException("unhandled cluster distribution state "+distribution.getName());
        }
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
