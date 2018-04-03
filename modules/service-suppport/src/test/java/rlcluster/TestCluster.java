package rlcluster;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.services.*;
import org.nustaq.kontraktor.services.rlclient.DataClient;
import org.nustaq.kontraktor.services.rlclient.DataShard;
import org.nustaq.reallive.api.RealLiveTable;
import org.nustaq.reallive.impl.tablespace.ClusteredTableSpaceClient;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TestCluster {

    public static void main(String[] args) throws IOException, InterruptedException {
        String name = new File("./").getCanonicalFile().getName();
        if ( ! "service-suppport".equals(name)) {
            System.out.println("pls run with working die service-support");
            System.exit(1);
        }

        // start Registry
        ServiceRegistry.main( new String[] {});
        Thread.sleep(1000);

        Executor ex = Executors.newCachedThreadPool();
        // Start Data Shards
        ClusterCfg cfg = ClusterCfg.read();
        for ( int i = 0; i < cfg.getDataCluster().getNumberOfShards(); i++ ) {
            final int finalI = i;
            ex.execute(() -> DataShard.main(new String[]{ "-host", "localhost", "-shardNo", ""+ finalI }));
        }
        Thread.sleep(2000);
        System.out.println("start test");

        ex.execute( () -> {
            // connect client
            ServiceActor serviceActor = ServiceActor.RunTCP(new String[]{"-host", "localhost"}, ServiceActor.class, ServiceArgs.class);
            DataClient dclient = (DataClient) serviceActor.getDataClient().await();
            RealLiveTable user = dclient.tbl("user");
            for ( int i = 0; i < 1; i++ ) {
                user.add(""+i, "name", "Ruedi", "value", "val"+i );
            }
            System.out.println("waiting ..");
            user.get("0").await();
            System.out.println("... done");
        });
    }

}
