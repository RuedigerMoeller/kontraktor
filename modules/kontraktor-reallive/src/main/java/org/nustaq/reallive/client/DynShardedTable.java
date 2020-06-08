package org.nustaq.reallive.client;

import org.nustaq.reallive.api.RealLiveTable;
import org.nustaq.reallive.api.TableDescription;
import org.nustaq.reallive.api.TableState;
import org.nustaq.reallive.server.actors.RealLiveTableActor;
import org.nustaq.reallive.server.dynamic.DynClusterTableDistribution;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

public class DynShardedTable extends ShardedTable{

    private final Supplier<DynClusterTableDistribution> tableDistributionSupplier;

    public DynShardedTable(RealLiveTable[] shards, TableDescription desc, Supplier<DynClusterTableDistribution> tableDistributionSupplier) {
        super(shards, desc);
        this.tableDistributionSupplier = tableDistributionSupplier;
    }

    protected RealLiveTable getTableForKey(String key) {
        List<TableState> states = tableDistributionSupplier.get().getStates();
        int h = key.hashCode();
        for (int i = 0; i < states.size(); i++) {
            TableState tableState = states.get(i);
            if ( tableState.getMapping().matches(h) ) {
                RealLiveTableActor associatedShard = tableState.getAssociatedTableShard();
                if ( associatedShard == null ) {
                    for (Iterator<RealLiveTable> iterator = shards.iterator(); iterator.hasNext(); ) {
                        RealLiveTableActor next = (RealLiveTableActor) iterator.next();
                        if ( next.__clientSideTag.equals(tableState.getAssociatedShardName()) ) {
                            tableState.associatedTableShard(next);
                            return next;
                        }
                    }
                }
                return associatedShard;
            }
        }
        return null;
    }

}
