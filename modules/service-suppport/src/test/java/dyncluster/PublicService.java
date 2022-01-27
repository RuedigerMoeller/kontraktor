package dyncluster;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.base.ServiceDescription;
import org.nustaq.kontraktor.services.ServiceActor;
import org.nustaq.kontraktor.services.ServiceArgs;
import org.nustaq.kontraktor.services.ServiceRegistry;
import org.nustaq.kontraktor.services.datacluster.dynamic.DynDataServiceRegistry;
import org.nustaq.reallive.api.RealLiveTable;
import org.nustaq.reallive.api.Record;

public class PublicService extends ServiceActor<PublicService> {

    @Override
    public IPromise init(ConnectableActor registryConnectable, ServiceArgs options, boolean autoRegister) {
        Promise res = new Promise();
        super.init(registryConnectable, options, autoRegister).then( (r,e) -> {
            runTestTask();
            res.complete(r,e);
        });
        return res;
    }

    private void runTestTask() {
        RealLiveTable test = dclient.tbl("testSpread");
        test.deepMerge(Record.from(
            "key", "1",
            "sample", 1
        ));
    }

    @Override
    protected String[] getRequiredServiceNames() {
        return new String[0];
    }

    @Override
    protected ServiceDescription createServiceDescription() {
        return new ServiceDescription("PublicService")
            .connectable(createDefaultConnectable());
    }

    @Override
    protected boolean isDynamicDataCluster() {
        return true;
    }

    /////////////////////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        ServiceArgs serviceArgs = ServiceRegistry.parseCommandLine(args,
            new String[] { "-host", "localhost", "-hostport", "7900" },
            ServiceArgs.New()
        );
        ServiceActor.RunTCP(
            serviceArgs, // args passed to the service
            PublicService.class, // name of the service actor implementing class
            DynDataServiceRegistry.class, // type of ServiceRegistry expected
            DEFAULT_START_TIMEOUT // time to wait for dependent services
        );
    }

}
