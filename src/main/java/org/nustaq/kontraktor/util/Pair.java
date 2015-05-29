package org.nustaq.kontraktor.util;

/**
 * Created by ruedi on 03/05/15.
 */
public class Pair<CAR, CDR> {

    CAR car;
    CDR cdr;

    public Pair(CAR CAR, CDR CDR) {
        this.car = CAR;
        this.cdr = CDR;
    }

    public CAR getFirst() {
        return car;
    }

    public CDR getSecond() {
        return cdr;
    }

    public CAR car() {
        return car;
    }

    public CDR cdr() {
        return cdr;
    }

    @Override
    public String toString() {
        return "Pair{" +
                   "first=" + car +
                   ", second=" + cdr +
                   '}';
    }
}
