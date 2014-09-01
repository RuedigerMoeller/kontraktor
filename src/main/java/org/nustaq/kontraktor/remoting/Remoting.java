package org.nustaq.kontraktor.remoting;

import org.nustaq.serialization.FSTConfiguration;

import java.rmi.Remote;

/**
 * Created by ruedi on 01.09.2014.
 */
public class Remoting {

    public static Remoting defaultInstance = new Remoting();

    public static Remoting This() {
        return defaultInstance;
    }

}
