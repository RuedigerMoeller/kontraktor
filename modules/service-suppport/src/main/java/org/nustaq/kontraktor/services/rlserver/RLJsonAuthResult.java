package org.nustaq.kontraktor.services.rlserver;

import java.io.Serializable;

public class RLJsonAuthResult implements Serializable {

    RLJsonSession session;

    public RLJsonAuthResult session(final RLJsonSession session) {
        this.session = session;
        return this;
    }

    public RLJsonSession getSession() {
        return session;
    }
}
