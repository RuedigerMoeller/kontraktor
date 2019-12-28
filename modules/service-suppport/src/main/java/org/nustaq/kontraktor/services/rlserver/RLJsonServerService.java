package org.nustaq.kontraktor.services.rlserver;

import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.services.ServiceActor;
import org.nustaq.kontraktor.services.ServiceArgs;
import org.nustaq.kontraktor.services.ServiceDescription;
import org.nustaq.kontraktor.services.rlclient.DataClient;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class RLJsonServerService extends ServiceActor<RLJsonServerService> {

    private RLJsonServer server;

    public static RLJsonServerService start(String a[]) {
        if ( ! new File("etc").exists() ) {
            System.out.println("Please start with working dir project root");
            System.exit(1);
        }
        return (RLJsonServerService) ServiceActor.RunTCP( a, RLJsonServerService.class, SingleProcessRLClusterArgs.class );
    }

    @Override
    protected String[] getRequiredServiceNames() {
        return new String[0];
    }

    @Override
    protected ServiceDescription createServiceDescription() {
        return new ServiceDescription("JSONServerService");
    }

    public void setWebServer(RLJsonServer self) {
        server = self;
    }

    @CallerSideMethod
    public DataClient getDClient() {
        return getActor().dclient;
    }
}
