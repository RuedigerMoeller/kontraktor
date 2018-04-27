package rlcluster;

import org.junit.Test;
import org.nustaq.kontraktor.services.*;
import org.nustaq.kontraktor.services.rlclient.DataClient;
import org.nustaq.kontraktor.services.rlclient.DataShard;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.api.RealLiveTable;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class TestCluster {

    public static class MyService extends ServiceActor<MyService> {

        @Override
        protected String[] getRequiredServiceNames() {
            return new String[0];
        }

        @Override
        protected ServiceDescription createServiceDescription() {
            return new ServiceDescription("MyService");
        }
    }

    static void queryLoop(RealLiveTable user)  {
        while (true) {
            CountDownLatch qq = new CountDownLatch(1);
            user.query("value == 'val13'", (r,e) -> {
                if ( r != null ) {
                    System.out.println("received "+r);
                } else {
                    qq.countDown();
                }
            });
            try {
                qq.await();
                Log.Info(TestCluster.class,"query done");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, Exception {
        checkDir();
        ServiceRegistry registryServer = startRegistry();

        AtomicReference<DataShard> someShard = initDataNodes();
        Log.Info(TestCluster.class,"start test");

        // connect client
        MyService serviceActor = (MyService) ServiceActor.RunTCP(new String[]{"-host", "localhost"}, MyService.class, ServiceArgs.class);
        DataClient dclient = serviceActor.getDataClient().await();
        RealLiveTable user = dclient.tbl("user");
        Log.Info(TestCluster.class,"start insert");
        for ( int i = 0; i < 10000; i++ ) {
            user.add(""+i, "name", "Ruedi", "value", "val"+i );
        }
        Log.Info(TestCluster.class,"waiting ..");
        user.get("0").await();
        Log.Info(TestCluster.class,"done");

        Executors.newCachedThreadPool().execute( () -> queryLoop(user) );
        Thread.sleep(5000);

        Log.Info(TestCluster.class,"terminating node ..."+someShard.get());
        someShard.get().close();
        someShard.get().stop();

//        Thread.sleep(2000);
//
//        Log.Info(TestCluster.class,"restarting node ... "+someShardNo.get());
//        DataShard.start(new String[]{"-host", "localhost", "-shardNo", "" + someShardNo.get() });

    }

    // return random datanode for failover tests
    protected static AtomicReference<DataShard> initDataNodes() throws InterruptedException {
        Executor ex = Executors.newCachedThreadPool();
        // Start Data Shards
        AtomicReference<DataShard> someShard = new AtomicReference<>();
        AtomicInteger someShardNo = new AtomicInteger();
        ClusterCfg cfg = ClusterCfg.read();
        for ( int i = 0; i < cfg.getDataCluster().getNumberOfShards(); i++ ) {
            final int finalI = i;
            ex.execute( () -> {
                DataShard sh = DataShard.start(new String[]{"-host", "localhost", "-shardNo", "" + finalI});
                if ( someShard.get() == null || Math.random() > .5)  {
                    someShard.set(sh);
                    someShardNo.set(finalI);
                }
            });
        }
        Thread.sleep(2000);
        return someShard;
    }

    protected static void checkDir() throws IOException {
        String name = new File("./").getCanonicalFile().getName();
        if ( ! "service-suppport".equals(name)) {
            System.out.println("pls run with working die service-support");
            System.exit(1);
        }
    }

    protected static ServiceRegistry startRegistry() throws InterruptedException {
        // start Registry
        ServiceRegistry registryServer = ServiceRegistry.start(new String[]{});
        Thread.sleep(1000);
        return registryServer;
    }

}
