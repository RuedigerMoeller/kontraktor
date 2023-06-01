package soa;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ServiceDescription;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.services.ServiceActor;
import org.nustaq.kontraktor.services.ServiceArgs;
import org.nustaq.kontraktor.services.ServiceRegistry;
import org.nustaq.kontraktor.services.datacluster.dynamic.DynDataServiceRegistry;

public class PublicService extends ServiceActor<PublicService> {

    @Override
    protected String[] getRequiredServiceNames() {
        return new String[0];
    }

    @Override
    protected ServiceDescription createServiceDescription() {
        return new ServiceDescription("PublicService")
            .connectable( new TCPConnectable( getClass(), "localhost", getPort() ));
    }

    @Override
    protected boolean isDynamicDataCluster() {
        return true;
    }

    ////////////////////////////////////// real api ////////////////////////////////////

    public void helloFrom(String simpleName) {
        System.out.println("** received hello from "+simpleName+" **");
    }

    public IPromise<Integer> benchMe(int value) {
        return resolve(value);
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
