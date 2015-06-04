package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import java.io.Serializable;

/**
 * Created by ruedi on 18/05/15.
 *
 * Describes a connectable remote or local actor. (implementations hold technical details such as host port etc.)
 *
 * To connect to a remote actor usually a connector is configured, then calling connect returns the reference to
 * the remote actor.
 *
 * In peer to peer actor remoting model, this can be passed around (over network) in order to
 * tell other actors/services on how to connect a certain actor directly (e.g. a Service Registry would
 * pass ConnectableActors to clients).
 *
 */
public interface ConnectableActor extends Serializable {

    <T> IPromise<T> connect( Callback<ActorClientConnector> disconnectCallback );
    ConnectableActor actorClass( Class actorClz );

}
