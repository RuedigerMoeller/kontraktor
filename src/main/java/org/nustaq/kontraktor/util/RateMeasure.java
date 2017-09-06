/*
Kontraktor Copyright (c) Ruediger Moeller, All rights reserved.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3.0 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

See https://www.gnu.org/licenses/lgpl.txt
*/

package org.nustaq.kontraktor.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

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
    int checkEachMask = 7;
    long statInterval = 1000;
    long lastRatePersecond;
    boolean print = false;
    boolean accumulate = true;
    long accumulated = 0;

    String name = "none";

    public RateMeasure(String name, long statInterval) {
        this.name = name;
        this.statInterval = statInterval;
    }

    public RateMeasure(String name) {
        this.name = name;
    }

    public long getAccumulated() {
        return accumulated;
    }

    /**
     * @return lastRate per interval
     */
    public long count() {
        int c = count.incrementAndGet();
        if ( (c & ~checkEachMask) == c ) {
            checkStats();
        }
        return lastRatePersecond;
    }

    public long count(int amount) {
        int c = count.addAndGet(amount);
        if ( (c & ~checkEachMask) == c ) {
            checkStats();
        }
        return lastRatePersecond;
    }

    /**
     * @return count of current unfinished interval
     */
    public int getOpenCount() {
        return count.get();
    }

    public RateMeasure print(final boolean print) {
        this.print = print;
        return this;
    }

    private void checkStats() {
        long now = System.currentTimeMillis();
        long diff = now-lastStats;
        if ( diff > statInterval ) {
            int val = count.get();
            lastRatePersecond = val *1000l/diff;
            lastStats = now;
            accumulated += val;
            count.set(0);
            statsUpdated(lastRatePersecond);
        }
    }

    /**
     * override this
     * @param lastRatePersecond
     */
    protected void statsUpdated(long lastRatePersecond) {
        if ( print )
            Log.Info(this,"***** Stats for "+name+":   "+lastRatePersecond+"   per second, acc:"+accumulated+" *********");
    }


    public RateMeasure lastStats(long lastStats) {
        this.lastStats = lastStats;
        return this;
    }

    public RateMeasure checkEachMask(int checkEachMask) {
        this.checkEachMask = checkEachMask;
        return this;
    }

    public RateMeasure statInterval(long statInterval) {
        this.statInterval = statInterval;
        return this;
    }

    public RateMeasure lastRatePersecond(long lastRatePersecond) {
        this.lastRatePersecond = lastRatePersecond;
        return this;
    }

    public RateMeasure accumulate(boolean accumulate) {
        this.accumulate = accumulate;
        return this;
    }

    public RateMeasure name(String name) {
        this.name = name;
        return this;
    }

    public RateMeasure accumulated(long accumulated) {
        this.accumulated = accumulated;
        return this;
    }
}