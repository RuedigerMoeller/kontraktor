package org.nustaq.kontraktor.services;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ServiceDescription;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;

/**
 * Created by ruedi on 29.05.17.
 */
public class PlainService extends ServiceActor<PlainService> {

    public static IPromise<PlainService> createService(String name, String[] required, ServiceArgs options ) {
        PlainService plainService = Actor.AsActor(PlainService.class);
        return plainService.initPlainService( name, required, options );
    }

    String serviceName;
    String[] requiredServices;

    public IPromise<PlainService> initPlainService(String name, String[] requiredServices, ServiceArgs options ) {
        if ( requiredServices == null ) {
            this.requiredServices = new String[0];
        } else {
            this.requiredServices = requiredServices;
        }
        this.serviceName = name;
        Promise p = new Promise();
        self().init( new TCPConnectable( ServiceRegistry.class, options.getRegistryHost(), options.getRegistryPort() ), options, true)
            .then( (r,e) -> {
                if ( e == null ) {
                    p.resolve(self());
                } else {
                    p.reject(e);
                }
            });
        return p;
    }

    @Override
    protected String[] getRequiredServiceNames() {
        return requiredServices;
    }

    @Override
    protected ServiceDescription createServiceDescription() {
        return new ServiceDescription( serviceName );
    }

}
