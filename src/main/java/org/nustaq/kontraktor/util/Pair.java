package org.nustaq.kontraktor.util;

/**
 * Created by ruedi on 03/05/15.
 */
public class Pair<FIRST,SECOND> {

    FIRST first;
    SECOND second;

    public Pair(FIRST first, SECOND second) {
        this.first = first;
        this.second = second;
    }

    public FIRST getFirst() {
        return first;
    }

    public SECOND getSecond() {
        return second;
    }

    @Override
    public String toString() {
        return "Pair{" +
                   "first=" + first +
                   ", second=" + second +
                   '}';
    }
}
