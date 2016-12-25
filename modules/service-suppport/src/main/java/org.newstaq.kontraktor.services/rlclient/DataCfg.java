package org.newstaq.kontraktor.services.rlclient;

import org.nustaq.reallive.interfaces.TableDescription;

import java.io.Serializable;

/**
 * Created by ruedi on 15.08.2015.
 */
public class DataCfg implements Serializable {

    String dataDir[] = {
        "/tmp/reallife", // can be same as table files will contain shardno also
    };

    TableDescription schema[] = { new TableDescription("dummy") };

    int threadsPerShard = 2;
    int shardQSize = 64_000;

    public int getNumberOfShards() {
        return dataDir.length;
    }

    public int getThreadsPerShard() {
        return threadsPerShard;
    }

    public int getShardQSize() {
        return shardQSize;
    }

    public String[] getDataDir() {
        return dataDir;
    }

    public DataCfg threadsPerShard(final int threadsPerShard) {
        this.threadsPerShard = threadsPerShard;
        return this;
    }

    public DataCfg shardQSize(final int shardQSize) {
        this.shardQSize = shardQSize;
        return this;
    }

    public DataCfg dataDir(final String[] dataDir) {
        this.dataDir = dataDir;
        return this;
    }

    public TableDescription[] getSchema() {
        return schema;
    }
}
