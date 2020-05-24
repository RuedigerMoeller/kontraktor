package org.nustaq.kontraktor.services.datacluster.dynamic.actions;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.services.ServiceDescription;
import org.nustaq.kontraktor.services.datacluster.dynamic.DynDataShard;

import java.io.Serializable;

public abstract class ClusterTableAction implements Serializable {

    String tableName;
    String shardName;
    String otherShard;

    public ClusterTableAction(String tableName, String shardName) {
        this.tableName = tableName;
        this.shardName = shardName;
    }

    public abstract IPromise action(DynDataShard remoteRef, ServiceDescription otherRef );

    public String getTableName() {
        return tableName;
    }

    public String getShardName() {
        return shardName;
    }

    public String getOtherShard() {
        return otherShard;
    }

    public ClusterTableAction tableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public ClusterTableAction shardName(String shardName) {
        this.shardName = shardName;
        return this;
    }

    public ClusterTableAction otherShard(String otherShard) {
        this.otherShard = otherShard;
        return this;
    }
}
