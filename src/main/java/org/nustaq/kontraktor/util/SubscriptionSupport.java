package org.nustaq.kontraktor.util;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.Remoted;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Single threaded !! For use inside an actor.
 *
 * See [..]/examples/docsamples/CBSampleServer
 */
public class SubscriptionSupport {

    private Map<Integer, Callback> subsMap = new HashMap<>();

    static class DummyCB implements Callback {

        public DummyCB() {
            this.timeStamp = System.currentTimeMillis();
        }

        protected long timeStamp;

        @Override
        public void complete(Object result, Object error) {
            //dummy
        }

        @Override
        public boolean isTerminated() {
            return System.currentTimeMillis()-timeStamp > 10000l;
        }

    }

    public Promise<Integer> createSubsId() {
        int id = (int) (Math.random()*Integer.MAX_VALUE);
        if ( getSubsMap().containsKey(id) )
            return createSubsId();
        getSubsMap().put(id,new DummyCB());
        return new Promise<>(id);
    }

    public Promise<Boolean> subscribe(int id, Callback cb) {
        if (getSubsMap().get(id) instanceof DummyCB == false )
            return new Promise(false);
        getSubsMap().put(id,cb);
        return new Promise(true);
    }

    public Promise<Boolean> unSubscribe(int id, Callback cb) {
        Callback removed = getSubsMap().remove(id);
        if ( removed != null ) // ensure remote mapping is cleaned up
            removed.finish();
        return new Promise( removed != null );
    }

    public void cleanUp() {
        Iterator<Map.Entry<Integer, Callback>> it = getSubsMap().entrySet().iterator();
        while( it.hasNext() ) {
            Map.Entry<Integer, Callback> next = it.next();
            if ( next.getValue().isTerminated() )
                it.remove();
        }
    }

    protected Map<Integer, Callback> getSubsMap() {
        return subsMap;
    }

}
