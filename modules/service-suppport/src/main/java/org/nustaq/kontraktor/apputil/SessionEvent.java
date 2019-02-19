package org.nustaq.kontraktor.apputil;

import java.io.Serializable;

public class SessionEvent implements Serializable {
    String type;
    Object data;

    public SessionEvent(String type, Object data) {
        this.type = type;
        this.data = data;
    }
}
