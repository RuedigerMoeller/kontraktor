package org.nustaq.kontraktor.routers;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ActorPublisher;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;

import java.util.Arrays;
import java.util.function.Consumer;

import static org.nustaq.kontraktor.Actors.AsActor;
import static org.nustaq.kontraktor.Actors.promise;
import static org.nustaq.kontraktor.routers.AbstractKrouter.CLIENT_PING_INTERVAL_MS;

/**
 * main entrance point for sarting Krouters, clients and services
 */
public class Routing {

    //////////////////////////////////////////////////// static API /////////////////////////////////////////////
    //
    //

    /**
     * start a Krouter
     * @param krouterClass
     * @param publisher
     * @param <T>
     * @return
     */
    public static <T extends AbstractKrouter> T start(Class<T> krouterClass, ActorPublisher... publisher) {
        T res = AsActor(krouterClass);
        res.init();
        for (int i = 0; i < publisher.length; i++) {
            ActorPublisher actorPublisher = publisher[i].facade(res);
            actorPublisher.publish( act -> {
                res.handleServiceDiscon(act);
            });
        }
        return res;
    }

    /**
     * connect a client to a remote Krouter
     *
     * @param connectable - the krouter to connect
     * @param disconnectCallback
     * @return
     */
    protected static Actor pinger;
    protected static Actor getPinger() {
        synchronized (AbstractKrouter.class) {
            if ( pinger == null ) {
                pinger = AsActor(Actor.class);
            }
            return pinger;
        }
    }

    public static IPromise<Object> connectClient(ConnectableActor connectable, Consumer<Actor> disconnectCallback) {
        Promise p = promise();
        connectable.connect(null, disconnectCallback ).then( (r,e) -> {
            if ( r != null )  {
                getPinger().cyclic(CLIENT_PING_INTERVAL_MS, () -> {

                    long[] paids = null;
                    if ( r.__clientConnection != null )
                        paids = r.__clientConnection.getRemotedActorIds();
//                    System.out.println("remoted ids:"+ Arrays.toString(paids));
//                    System.out.println("published ids:"+ Arrays.toString(r.__clientConnection.getPublishedActorIds()));
                    r.router$clientPing(System.currentTimeMillis(),paids);
                });
            }
            p.complete(r,e);
        });
        return p;
    }

    /**
     * publish and register a service at a remote Krouter
     *
     * @param connectable - the krouter to connect
     * @param service - the service to publish
     * @param disconnectCallback
     * @return
     */
    public static IPromise<Object> registerService(ConnectableActor connectable, Actor service, Consumer<Actor> disconnectCallback) {
        Promise p = promise();
        service.getActor().zzRoutingGCEnabled = true;
        service.getActorRef().zzRoutingGCEnabled = true;
        service.execute( () -> {
            connectable
                .connect(null, (Consumer<Actor>) disconnectCallback)
                .then( (r,e) -> {
                    if ( r != null ) {
                        try {
                            ((AbstractKrouter) r).router$RegisterService(service.getUntypedRef()).await();
                        } catch (Exception ex) {
                            p.complete(null,ex);
                            return;
                        }
                    }
                    p.complete(r,e);
                });
        });
        return p;
    }

    //
    //
    //////////////////////////////////////////////////// static API /////////////////////////////////////////////

}
