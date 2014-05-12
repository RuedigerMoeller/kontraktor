package de.ruedigermoeller.kontraktor;

/**
 * Created by moelrue on 12.05.2014.
 */
public interface ActorRunnable<T,R> {
    public void run(T actorAccess, Actor actorImpl, Callback<R> resultReceiver);
}
