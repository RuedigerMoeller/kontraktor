package de.ruedigermoeller.disruptorbench;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ruedi on 21.04.14.
 */
public class LockFreeSharedData implements SharedData {
    ConcurrentHashMap aMap;

    public LockFreeSharedData() {
        this.aMap = new ConcurrentHashMap();
        for ( int i = 0; i < 1000; i++ ) {
            aMap.put(i,i);
        }
    }

    @Override
    public Integer lookup(int i) {
        return (Integer) aMap.get(i);
    }
}
