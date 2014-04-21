package de.ruedigermoeller.disruptorbench;

/**
 * Created by ruedi on 21.04.14.
 */
public class SynchronizedSharedData extends SingleThreadedSharedData {

    @Override
    public Integer lookup(int i) {
        synchronized (this) {
            return super.lookup(i);
        }
    }
}
