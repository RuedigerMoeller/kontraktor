package org.nustaq.kontraktor.util;

/**
 * Created with IntelliJ IDEA.
 * User: ruedi
 * Date: 10/15/13
 * Time: 7:47 PM
 *
 * copied form fast-cast
 */
public class RateMeasure {

    int count;
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
        count++;
        if ( (count & ~checkEachMask) == count ) {
            checkStats();
        }
    }

    private void checkStats() {
        long now = System.currentTimeMillis();
        long diff = now-lastStats;
        if ( diff > statInterval ) {
            lastRatePersecond = count*1000l/diff;
            lastStats = now;
            count = 0;
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