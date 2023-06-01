package soa;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.base.ServiceDescription;
import org.nustaq.kontraktor.services.ServiceActor;
import org.nustaq.kontraktor.services.ServiceArgs;
import org.nustaq.kontraktor.services.datacluster.dynamic.DynDataServiceRegistry;

/**
 * a service registering at registry, but it does not expose itself to other services of the cluster.
 * (usually does only make sense if a reallive db cluster is present)
 */
public class PrivateService extends ServiceActor<PrivateService> {

    @Override
    public IPromise init(ConnectableActor registryConnectable, ServiceArgs options, boolean autoRegister) {
        Promise res = new Promise();
        super.init(registryConnectable, options, autoRegister).then( (r,e) -> {
           PublicService remoteService = getService("PublicService");
           remoteService.helloFrom( getClass().getSimpleName() );
           res.complete(r,e);
        });
        return res;
    }

    @Override
    protected String[] getRequiredServiceNames() {
        return new String[] {"PublicService"};
    }

    @Override
    protected ServiceDescription createServiceDescription() {
        return new ServiceDescription("PrivateService");
    }

    @Override
    protected boolean isDynamicDataCluster() {
        return true;
    }

    public static void main(String[] args) {
        ServiceActor.RunTCP(
            args, // args passed to the service
            PrivateService.class, // name of the service actor implementing class
            ServiceArgs.class, // class parsing/holding commandline args (can be altered to add service specific arguments, need subclass of ServiceArgs)
            DynDataServiceRegistry.class // type of ServiceRegistry expected
        );
    }
}
