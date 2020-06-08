package org.nustaq.kontraktor.services.rlserver;

import org.nustaq.kontraktor.Callback;
import org.nustaq.reallive.api.Subscriber;

public class JsonSubsEntry {
    Callback feCB;
    Subscriber subs;

    public JsonSubsEntry(Callback feCB, Subscriber subs) {
        this.feCB = feCB;
        this.subs = subs;
    }

    public Callback getFeCB() {
        return feCB;
    }

    public Subscriber getSubs() {
        return subs;
    }
}
