package rljson;

import org.nustaq.kontraktor.services.ServiceActor;
import org.nustaq.kontraktor.services.ServiceArgs;
import org.nustaq.kontraktor.remoting.base.ServiceDescription;
import org.nustaq.kontraktor.services.datacluster.dynamic.DynDataServiceRegistry;
import org.nustaq.kontraktor.services.rlclient.DataClient;
import org.nustaq.reallive.api.RLLimitedPredicate;
import org.nustaq.reallive.api.RealLiveTable;
import org.nustaq.reallive.api.Record;

import java.util.List;

public class DynClusterTestClient extends ServiceActor<DynClusterTestClient> {

    @Override
    protected String[] getRequiredServiceNames() {
        return new String[0];
    }

    protected boolean isDynamicDataCluster() {
        return true;
    }

    @Override
    protected ServiceDescription createServiceDescription() {
        return new ServiceDescription("DynTestClient");
    }

    public static final int REC_TO_ADD = 100_000;
    public static void main(String[] args) {
        ServiceActor serviceActor = ServiceActor.RunTCP(args, DynClusterTestClient.class, ServiceArgs.class, DynDataServiceRegistry.class);
        DataClient dclient = (DataClient) serviceActor.getDataClient().await();
        RealLiveTable feed = dclient.tbl("feed");
        boolean modify = false;
        boolean slowQuery = true;
        int tim = 999;//(int) System.currentTimeMillis();
        if ( modify ) {
            for(int i = 0; i < REC_TO_ADD; i++ )
            {
                feed.update(""+i+tim, "a", i, "b", "Hello", "c", 1000-i );
                if ( i%10_000 == 9999 )
                    feed.ping().await();
            }
            for( int i = 0; i < REC_TO_ADD; i++ )
            {
                feed.update(""+i+tim, "a", i, "b", "Hello Feed", "c", 1000-i );
                if ( i%10_000 == 9999 )
                    feed.ping().await();
            }
        }
        feed.ping().await();

        List<Record> c = feed.queryList(x -> x.getLong("c") == 10).await();
        System.out.println(c);

        if ( slowQuery ) {
            feed.subscribeOn(new RLLimitedPredicate<>(1000, rec -> true), (change) -> {
                if ( change != null && ! change.isDoneMsg()) {
                    System.out.println("change "+change);
                } else {
                    System.out.println("done "+change);
                }
            });
        }

        System.out.println("main done");
    }
}
