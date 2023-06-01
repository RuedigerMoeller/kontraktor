package soa;

import io.undertow.server.HttpServerExchange;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.base.ServiceDescription;
import org.nustaq.kontraktor.services.ServiceActor;
import org.nustaq.kontraktor.services.ServiceArgs;
import org.nustaq.kontraktor.services.ServiceRegistry;
import org.nustaq.kontraktor.services.datacluster.dynamic.DynDataServiceRegistry;

public class ServiceWithWebServer extends ServiceActor<ServiceWithWebServer> {

    ChainedPublicService chainedPublicService;

    @Override
    public IPromise init(ConnectableActor registryConnectable, ServiceArgs options, boolean autoRegister) {
        Promise res = new Promise();
        super.init(registryConnectable, options, autoRegister).then( (r,e) -> {
            // after init start webserver
            WebServer webServer = AsActor(WebServer.class);
            webServer.init(self());

            chainedPublicService = getService("ChainedPublicService");

            res.complete(r,e);
        });
        return res;
    }

    public IPromise<Integer> benchMe(int value) {
        Promise res = new Promise();
        chainedPublicService.benchMe(value).then(res); // just forward
        return res;
    }

    @Override
    protected String[] getRequiredServiceNames() {
        return new String[] {"ChainedPublicService"};
    }

    @Override
    protected ServiceDescription createServiceDescription() {
        return new ServiceDescription("ServiceWithWebServer")
            .connectable(createDefaultConnectable());
    }

    @Override
    protected boolean isDynamicDataCluster() {
        return true;
    }

    public static void main(String[] args) {
        ServiceArgs serviceArgs = ServiceRegistry.parseCommandLine(args,
            new String[] { "-host", "localhost", "-hostport", "7902" },
            ServiceArgs.New()
        );
        ServiceActor.RunTCP(
            serviceArgs, // args passed to the service
            ServiceWithWebServer.class, // name of the service actor implementing class
            DynDataServiceRegistry.class, // type of ServiceRegistry expected
            DEFAULT_START_TIMEOUT // time to wait for dependent services
        );
    }

}
