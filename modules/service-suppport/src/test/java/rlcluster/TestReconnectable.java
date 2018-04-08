package rlcluster;

import org.nustaq.kontraktor.remoting.base.ReconnectableRemoteRef;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.services.ServiceArgs;
import org.nustaq.kontraktor.services.ServiceRegistry;

public class TestReconnectable {

    public static void main(String[] args) throws InterruptedException {

        ServiceArgs sa = new ServiceArgs(); // grab serviceregistry default options

        TCPConnectable sr = new TCPConnectable()
            .actorClass(ServiceRegistry.class)
            .host(sa.getRegistryHost())
            .port(sa.getRegistryPort());

        ReconnectableRemoteRef<ServiceRegistry> reg = new ReconnectableRemoteRef( sr, ReconnectableRemoteRef.loggingListener );
        Thread.sleep(500_000);

        // start Registry
//        ServiceRegistry serverRegistry = ServiceRegistry.start(new String[]{});
//        Thread.sleep(1000);

    }
}
