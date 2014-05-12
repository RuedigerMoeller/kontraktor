package de.ruedigermoeller.kontraktor;

/**
 * Created by moelrue on 12.05.2014.
 */
public interface ActorRunnable<T> {
    public void run(Object actorAccess, Actor actorImpl, Callback<T> resultReceiver);
}
