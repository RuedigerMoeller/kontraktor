package org.nustaq.kontraktor.services.datacluster.dynamic;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ServiceDescription;
import org.nustaq.reallive.server.dynamic.actions.ClusterTableAction;

public class MoveHashShardsAction extends ClusterTableAction {

    int hashShards2Move[];

    public MoveHashShardsAction(int[] hashShards2Move, String tableName, String sendShardName, String receiveShardName) {
        super(tableName, sendShardName);
        otherShard = receiveShardName;
        this.hashShards2Move = hashShards2Move;
    }

    @Override
    public IPromise action(Actor remoteRef, ServiceDescription otherRef) {
        return ((DynDataShard)remoteRef)._moveHashShards( tableName, hashShards2Move, otherRef );
    }

}
