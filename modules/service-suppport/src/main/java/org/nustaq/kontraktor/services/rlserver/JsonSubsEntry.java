package org.nustaq.kontraktor.services.rlserver;

import org.nustaq.kontraktor.Callback;
import org.nustaq.reallive.api.Subscriber;

public class JsonSubsEntry<T> {
    Callback<T> feCB;
    Subscriber subs;

    public JsonSubsEntry(Callback<T> feCB, Subscriber subs) {
        this.feCB = feCB;
        this.subs = subs;
    }

    public Callback<T> getFeCB() {
        return feCB;
    }

    public Subscriber getSubs() {
        return subs;
    }
}
