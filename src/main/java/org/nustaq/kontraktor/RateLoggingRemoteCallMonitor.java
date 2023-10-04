package org.nustaq.kontraktor;

import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;
import org.nustaq.kontraktor.util.RateMeasure;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class RateLoggingRemoteCallMonitor implements RemoteCallMonitor {

    ConcurrentMap<Class,RateMeasure> rateMeasureMap = new ConcurrentHashMap<>();

    long intervalMillis;

    public RateLoggingRemoteCallMonitor(long intervalMillis) {
        this.intervalMillis = intervalMillis;
    }

    @Override
    public <SELF extends Actor> void remoteCallObserved(Actor selfActor, ObjectSocket objSocket, RemoteCallEntry rce, Object authContext) {
        RateMeasure rateMeasure = rateMeasureMap.get(selfActor.getClass());
        if ( rateMeasure == null ) {
            rateMeasure = new RateMeasure(selfActor.getClass().getSimpleName(), intervalMillis).print(true);
            rateMeasureMap.put(selfActor.getClass(),rateMeasure);
        }
        rateMeasure.count();
    }

    public static void main(String[] args) throws InterruptedException {
        Actor actor = Actors.AsActor(Actor.class);
        RateLoggingRemoteCallMonitor mon = new RateLoggingRemoteCallMonitor(TimeUnit.SECONDS.toMillis(1));
        for (int i = 0; i < 1_000_000_000; i++) {
            mon.remoteCallObserved( actor, null, null, null );
            Thread.sleep(1);
        }
        System.exit(1);
    }

}
