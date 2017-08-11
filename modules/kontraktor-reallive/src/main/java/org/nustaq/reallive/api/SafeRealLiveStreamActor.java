package org.nustaq.reallive.api;

import org.nustaq.kontraktor.Callback;

import java.text.ParseException;

public interface SafeRealLiveStreamActor {
    void query(String query, Callback<Record> cb) throws ParseException;
}
