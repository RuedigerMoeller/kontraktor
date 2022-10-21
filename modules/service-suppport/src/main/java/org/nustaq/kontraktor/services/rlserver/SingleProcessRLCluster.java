package org.nustaq.kontraktor.services.rlserver;

import com.mongodb.ConnectionString;
import com.mongodb.client.model.Indexes;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import org.nustaq.kontraktor.services.ClusterCfg;
import org.nustaq.kontraktor.services.ServiceRegistry;
import org.nustaq.kontraktor.services.datacluster.DataCfg;
import org.nustaq.kontraktor.services.datacluster.DataShard;
import org.nustaq.kontraktor.services.datacluster.DataShardArgs;
import org.nustaq.reallive.api.RecordStorage;
import org.nustaq.reallive.api.TableDescription;
import org.nustaq.reallive.client.EmbeddedRealLive;
import org.nustaq.reallive.server.storage.CachedOffHeapStorage;
import org.nustaq.reallive.server.storage.HeapRecordStorage;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.nustaq.kontraktor.services.ServiceRegistry.parseCommandLine;

public class SingleProcessRLCluster {

    private static SingleProcessRLClusterArgs options;
    public static MongoClient mongo;
    public static MongoDatabase mongoDB;

    public static void main(String[] args) throws InterruptedException {
        if ( ! new File("./etc").exists() ) {
            System.out.println("Please start with working dir [project]");
            System.exit(1);
        }

        options = (SingleProcessRLClusterArgs) parseCommandLine(args,null,new SingleProcessRLClusterArgs());
        SimpleRLConfig scfg = SimpleRLConfig.read();

        ClusterCfg cfg = new ClusterCfg();
        DataCfg datacfg = new DataCfg();
        datacfg.schema(scfg.tables);
        String dirs[] = new String[scfg.numNodes];
        for (int i = 0; i < dirs.length; i++) {
            dirs[i] = scfg.dataDir;
        }
        datacfg.dataDir(dirs);
        cfg.dataCluster(datacfg);

        // start Registry
        ServiceRegistry.start( options,cfg);
        Thread.sleep(1000);

        Executor ex = Executors.newCachedThreadPool();
        // Start Data Shards

        for ( int i = 0; i < cfg.getDataCluster().getNumberOfShards(); i++ ) {
            final int finalI = i;
            ex.execute(() -> DataShard.start(DataShardArgs.from(options,finalI)));
        }
    }
}

