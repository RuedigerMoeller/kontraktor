package org.nustaq.kontraktor.weblication;

import java.io.Serializable;

/**
 * Created by ruedi on 20.06.17.
 */
public class BasicWebAppConfig implements Serializable {


    int numSessionThreads = 4;

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
