package org.nustaq.reallive.server.dynamic.actions;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ServiceDescription;

import java.io.Serializable;

public abstract class ClusterTableAction implements Serializable {

    protected String tableName;
    protected String shardName; // node processing this action
    protected String otherShard; // node to connect to in case

    public ClusterTableAction(String tableName, String shardName) {
        this.tableName = tableName;
        this.shardName = shardName;
    }

    public abstract IPromise action(Actor remoteRef /*datashard*/, ServiceDescription otherRef );

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
