package de.ruedigermoeller.abstraktor;

/**
 * Created by ruedi on 1/8/14.
 */
public interface ActorScheduler {
    Dispatcher newDispatcher();
    Dispatcher aquireDispatcher();
}
