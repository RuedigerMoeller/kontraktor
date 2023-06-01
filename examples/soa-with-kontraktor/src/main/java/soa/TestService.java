package soa;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.base.ServiceDescription;
import org.nustaq.kontraktor.services.ServiceActor;
import org.nustaq.kontraktor.services.ServiceArgs;
import org.nustaq.kontraktor.services.datacluster.dynamic.DynDataServiceRegistry;
import org.nustaq.reallive.api.RealLiveTable;

public class TestService extends ServiceActor<TestService> {

    @Override
    public IPromise init(ConnectableActor registryConnectable, ServiceArgs options, boolean autoRegister) {
        Promise res = new Promise();
        super.init(registryConnectable, options, autoRegister).then( (r,e) -> {
            res.complete(r,e);
            runTest();
        });
        return res;
    }

    private void runTest() {
        String key = "45";
        RealLiveTable test = dclient.tbl("test");
        test.get(key).then( (r,e) -> {
            if ( r != null )
                System.out.println("Found record "+r.toPrettyString());
            else
                System.out.println("record "+key+" not found");
        });
        test.remove(key);
        test.subscribeOn( rec -> !rec.getBool("flag"), change -> {
            System.out.println("received "+change);
        });
        delayed( 2000, () -> {
            System.out.println("create "+key);
            test.update(key, "dummy", true );
        });
        delayed( 3000, () -> {
            System.out.println("updating set true expect remove");
            test.update(key, "flag", true );
        });
        delayed( 4000, () -> {
            System.out.println("updating set false expect add");
            test.update(key, "flag", false );
        });
        delayed( 5000, () -> {
            System.out.println("updating set false expect add");
            test.update(key, "flag", true );
        });
    }

    @Override
    protected ServiceDescription createServiceDescription() {
        return new ServiceDescription("TestService");
    }

    @Override
    protected boolean isDynamicDataCluster() {
        return true;
    }

    @Override
    protected String[] getRequiredServiceNames() {
        return new String[0];
    }

    public static void main(String[] args) {
        ServiceActor.RunTCP(
            args, // args passed to the service
            TestService.class, // name of the service actor implementing class
            ServiceArgs.class, // class parsing/holding commandline args (can be altered to add service specific arguments, need subclass of ServiceArgs)
            DynDataServiceRegistry.class // type of ServiceRegistry expected
        );
    }
}
