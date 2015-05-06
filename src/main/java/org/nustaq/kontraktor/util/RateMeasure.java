package org.nustaq.kontraktor.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: ruedi
 * Date: 10/15/13
 * Time: 7:47 PM
 *
 * copied form fast-cast
 */
public class RateMeasure {

    AtomicInteger count = new AtomicInteger(0);
    long lastStats;
    int checkEachMask = 127;
    long statInterval = 1000;
    long lastRatePersecond;

    String name = "none";

    public RateMeasure(String name, long statInterval) {
        this.name = name;
        this.statInterval = statInterval;
    }

    public RateMeasure(String name) {
        this.name = name;
    }

    public void count() {
        int c = count.incrementAndGet();
        if ( (c & ~checkEachMask) == c ) {
            checkStats();
        }
    }

    private void checkStats() {
        long now = System.currentTimeMillis();
        long diff = now-lastStats;
        if ( diff > statInterval ) {
            lastRatePersecond = count.get()*1000l/diff;
            lastStats = now;
            count.set(0);
            statsUpdated(lastRatePersecond);
        }
    }

    /**
     * override this
     * @param lastRatePersecond
     */
    protected void statsUpdated(long lastRatePersecond) {
        Log.Info(this,"***** Stats for "+name+":   "+lastRatePersecond+"   per second *********");
    }


}