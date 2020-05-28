package org.nustaq.kontraktor.services.datacluster.dynamic;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ServiceDescription;
import org.nustaq.reallive.server.dynamic.actions.ClusterTableAction;
import org.nustaq.reallive.server.storage.ClusterTableRecordMapping;

/**
 * just assigns a bucket mapping to a shard#table. used to initialize empty nodes or unused shardno's
 */
public class AssignMappingAction extends ClusterTableAction {

    public AssignMappingAction(String tableName, String shardName, ClusterTableRecordMapping mapping) {
        super(tableName, shardName);
        this.mapping = mapping;
    }

    ClusterTableRecordMapping mapping;

    @Override
    public IPromise action(Actor remoteRef, ServiceDescription otherRef) {
        return ((DynDataShard)remoteRef)._setMapping( tableName,mapping);
    }

    @Override
    public String toString() {
        return "AssignMappingAction{ " +
            "\nmapping=" + mapping +
            "\n, tableName='" + tableName + '\'' +
            "\n, shardName='" + shardName + '\'' +
            '}';
    }
}
