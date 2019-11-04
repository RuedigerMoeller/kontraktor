package org.nustaq.kontraktor.services.rlserver;

import org.nustaq.kontraktor.services.ClusterCfg;
import org.nustaq.kontraktor.services.ServiceRegistry;
import org.nustaq.kontraktor.services.rlclient.DataCfg;
import org.nustaq.kontraktor.services.rlclient.DataShard;
import org.nustaq.kontraktor.services.rlclient.DataShardArgs;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.nustaq.kontraktor.services.ServiceRegistry.parseCommandLine;

public class SingleProcessRLCluster {

    private static SingleProcessRLClusterArgs options;

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

