package org.nustaq.reallive.api;

import org.nustaq.kontraktor.Callback;
import org.nustaq.reallive.query.QParseException;


public interface SafeRealLiveStreamActor {
    void query(String query, Callback<Record> cb) throws QParseException;
}
