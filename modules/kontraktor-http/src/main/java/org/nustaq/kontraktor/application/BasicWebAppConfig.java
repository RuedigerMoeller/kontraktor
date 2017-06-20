package org.nustaq.kontraktor.application;

import java.io.Serializable;

/**
 * Created by ruedi on 20.06.17.
 */
public class BasicWebAppConfig implements Serializable {


    int numSessionThreads;

    public int getNumSessionThreads() {
        return numSessionThreads;
    }

    public void setNumSessionThreads(int numSessionThreads) {
        this.numSessionThreads = numSessionThreads;
    }

    public BasicWebAppConfig numSessionThreads(int numSessionThreads) {
        this.numSessionThreads = numSessionThreads;
        return this;
    }
}
