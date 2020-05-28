package org.nustaq.reallive.client;

import org.nustaq.reallive.api.RealLiveTable;
import org.nustaq.reallive.api.TableDescription;
import org.nustaq.reallive.server.actors.TableSpaceActor;
import org.nustaq.reallive.server.dynamic.DynClusterDistribution;

import java.util.function.Supplier;

public class DynTableSpaceSharding extends TableSpaceSharding {

    private final Supplier<DynClusterDistribution> distributionSupplier;

    public DynTableSpaceSharding(TableSpaceActor[] shards, Supplier<DynClusterDistribution> distributionSupplier) {
        super(shards);
        this.distributionSupplier = distributionSupplier;
    }

    protected ShardedTable createShardedTable(TableDescription desc, RealLiveTable[] tableShards) {
        return new DynShardedTable(tableShards, desc, () -> distributionSupplier.get().get(desc.getName()));
    }

}
