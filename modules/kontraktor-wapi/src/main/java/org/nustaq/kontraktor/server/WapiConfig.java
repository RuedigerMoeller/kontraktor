package org.nustaq.kontraktor.server;

import java.io.Serializable;

/**
 * Created by ruedi on 09.03.17.
 */
public class WapiConfig implements Serializable {

    public static WapiConfig read() {
        return new WapiConfig();
    }

    public static boolean isDirty() {
        return false;
    }
}
