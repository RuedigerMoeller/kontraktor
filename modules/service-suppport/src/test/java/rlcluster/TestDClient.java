package rlcluster;

import org.nustaq.kontraktor.services.ServiceActor;
import org.nustaq.kontraktor.services.ServiceArgs;
import org.nustaq.kontraktor.services.ServiceRegistry;
import org.nustaq.kontraktor.services.rlclient.DataClient;
import org.nustaq.kontraktor.services.rlclient.DataShard;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.api.RealLiveTable;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class TestDClient extends TestCluster {

    public static void main(String[] args) throws IOException, InterruptedException, Exception {
        checkDir();
        // connect client
        TestCluster.MyService serviceActor = (TestCluster.MyService) ServiceActor.RunTCP(new String[]{"-host", "localhost"}, TestCluster.MyService.class, ServiceArgs.class);
        DataClient dclient = serviceActor.getDataClient().await();
        RealLiveTable user = dclient.tbl("user");
        Log.Info(TestCluster.class,"start insert");
        for ( int i = 0; i < 15000; i++ ) {
            user.add(""+i, "name", "Ruedi", "value", "val"+i );
        }
        Log.Info(TestCluster.class,"waiting ..");
        user.get("0").await();
        Log.Info(TestCluster.class,"done");

        user.subscribeOn( rec -> "val13".equals(rec.getString("value")), change -> {
            System.out.println("CHANGE val13 "+change);
        } );

        user.subscribeOn( rec -> true, change -> {
            System.out.println("CHANGE ALL "+change);
        } );

        while( true ) {
            user.update(""+13, "name", "Ruedi"+Math.random());
            Thread.sleep(1000);
            user.add(""+Math.random(), "name", "Ruedi", "value", "val"+Math.random());
            Thread.sleep(1000);
        }
//        Executors.newCachedThreadPool().execute( () -> queryLoop(user) );

    }
}

