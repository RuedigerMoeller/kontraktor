package org.nustaq.kontraktor.impl;

import java.io.Serializable;

/**
* Created by ruedi on 02/04/15.
*/
public class SchedulingReport implements Serializable {

    int numDispatchers;
    int defQSize;
    int isolatedThreads;

    public SchedulingReport() {
    }

    public SchedulingReport(int numDispatchers, int defQSize, int isolatedThreads) {
        this.numDispatchers = numDispatchers;
        this.defQSize = defQSize;
        this.isolatedThreads = isolatedThreads;
    }

    public int getNumDispatchers() {
        return numDispatchers;
    }

    public int getDefQSize() {
        return defQSize;
    }
}
