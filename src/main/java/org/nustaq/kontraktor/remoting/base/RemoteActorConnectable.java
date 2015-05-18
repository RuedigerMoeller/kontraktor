package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;

import java.io.Serializable;

/**
 * Created by ruedi on 18/05/15.
 *
 * Describes a connectable remote actor.
 *
 */
public interface RemoteActorConnectable<T extends Actor> extends Serializable {

    IPromise<T> connect( Callback<ActorClientConnector> disconnectCallback );

}
