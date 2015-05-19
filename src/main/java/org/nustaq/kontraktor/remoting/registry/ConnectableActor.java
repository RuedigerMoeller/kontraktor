package org.nustaq.kontraktor.remoting.registry;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;

import java.io.Serializable;

/**
 * Created by ruedi on 18/05/15.
 *
 * Describes a connectable remote or local actor. In peer to peer actor remoting model, this
 * can be passed around in order to tell other actors/services on how to reach/connect a certain actor.
 *
 */
public interface ConnectableActor<T extends Actor> extends Serializable {

    IPromise<T> connect( Callback<ActorClientConnector> disconnectCallback );
    IPromise close();

}
