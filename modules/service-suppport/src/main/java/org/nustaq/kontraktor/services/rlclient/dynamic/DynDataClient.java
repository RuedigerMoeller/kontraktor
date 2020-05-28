package org.nustaq.kontraktor.services.rlclient.dynamic;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.services.ServiceActor;
import org.nustaq.kontraktor.services.datacluster.DataCfg;
import org.nustaq.reallive.server.dynamic.DynClusterDistribution;
import org.nustaq.kontraktor.services.datacluster.dynamic.DynDataServiceRegistry;
import org.nustaq.kontraktor.services.rlclient.DataClient;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.api.TableDescription;
import org.nustaq.reallive.client.DynTableSpaceSharding;
import org.nustaq.reallive.client.TableSpaceSharding;
import org.nustaq.reallive.server.actors.TableSpaceActor;

import java.util.HashMap;

public class DynDataClient extends DataClient<DynDataClient> {

    DynClusterDistribution currentMapping;

    public IPromise connect(DataCfg config, TableSpaceActor shards[], ServiceActor hostingService ) {
        this.config = config;
        this.hostingService=hostingService;
        this.shards = shards;
        syncTableAccess = new HashMap();
        tableSpaceSharding = createTableSpaceSharding(shards);
        tableSpaceSharding.init().await();
        if (!isDynDataCluster()) {
            Log.Error(this,"FATAL: not running with dynamic registry");
            delayed(1000, () -> System.exit(1));
        }
        hostingService.addServiceEventListener((event, arg) -> handleServiceEvent((String) event, arg));
        TableDescription[] schema = config.getSchema();
        return all( schema.length, i -> {
            TableDescription desc = schema[i];
            return initTable(desc);
        });
    }

    public void setInitialMapping(DynClusterDistribution mapping ) {
        this.currentMapping = mapping;
    }

    protected boolean isDynDataCluster() {
        return hostingService.getServiceRegistry() instanceof DynDataServiceRegistry;
    }

    protected void handleServiceEvent(String event, Object arg) {
        if ( event.equals(DynDataServiceRegistry.RECORD_DISTRIBUTION) ) {
            currentMapping = (DynClusterDistribution) arg;
        }
    }

    protected TableSpaceSharding createTableSpaceSharding(TableSpaceActor[] shards) {
        return new DynTableSpaceSharding(shards, () -> currentMapping );
    }

}
