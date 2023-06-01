package soa;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.base.ServiceDescription;
import org.nustaq.kontraktor.services.ServiceActor;
import org.nustaq.kontraktor.services.ServiceArgs;
import org.nustaq.kontraktor.services.ServiceRegistry;
import org.nustaq.kontraktor.services.datacluster.dynamic.DynDataServiceRegistry;

public class ChainedPublicService extends ServiceActor<ChainedPublicService> {

    PublicService publicService;

    @Override
    public IPromise init(ConnectableActor registryConnectable, ServiceArgs options, boolean autoRegister) {
        Promise res = new Promise();
        super.init(registryConnectable, options, autoRegister).then( (r,e) -> {
            publicService = getService("PublicService");
        });
        return res;
    }

    @Override
    protected String[] getRequiredServiceNames() {
        return new String[] {"PublicService"};
    }

    @Override
    protected ServiceDescription createServiceDescription() {
        return new ServiceDescription("ChainedPublicService")
            .connectable(createDefaultConnectable());
    }

    @Override
    protected boolean isDynamicDataCluster() {
        return true;
    }

    public IPromise<Integer> benchMe(int value) {
        Promise res = new Promise();
        publicService.benchMe(value).then(res); // just forward
        return res;
    }

    public static void main(String[] args) {
        ServiceArgs serviceArgs = ServiceRegistry.parseCommandLine(args,
            new String[]{"-host", "localhost", "-hostport", "7901"},
            ServiceArgs.New()
        );
        ServiceActor.RunTCP(
            serviceArgs, // args passed to the service
            ChainedPublicService.class, // name of the service actor implementing class
            DynDataServiceRegistry.class, // type of ServiceRegistry expected
            DEFAULT_START_TIMEOUT // time to wait for dependent services
        );
    }
}
