package dyncluster;

import org.nustaq.kontraktor.services.ClusterCfg;
import org.nustaq.kontraktor.services.RegistryArgs;
import org.nustaq.kontraktor.services.ServiceRegistry;
import org.nustaq.kontraktor.services.datacluster.dynamic.DynDataServiceRegistry;

public class MyServiceRegistry extends DynDataServiceRegistry {

    private static final boolean AUTO_BALANCE = false;

    public static void main(String[] args) {
        options = (RegistryArgs) ServiceRegistry.parseCommandLine(
            args,
            new String[]{ "-dumpServices"}, // can also check http://localhost:1113/mon
            RegistryArgs.New()
        );

        ClusterCfg cfg = ClusterCfg.read();
        DynDataServiceRegistry reg = (DynDataServiceRegistry) ServiceRegistry.start(options, cfg, MyServiceRegistry.class);

        if ( AUTO_BALANCE ) {
            // as we do not run any datanodes, we can immediately trigger balancing
            // this is required as all connecting nodes will only start up once a balance is triggered
            reg.balanceDynShards();
        }
    }

}
