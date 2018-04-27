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
        for ( int i = 0; i < 10000; i++ ) {
            user.add(""+i, "name", "Ruedi", "value", "val"+i );
        }
        Log.Info(TestCluster.class,"waiting ..");
        user.get("0").await();
        Log.Info(TestCluster.class,"done");

        Executors.newCachedThreadPool().execute( () -> queryLoop(user) );

    }

}
