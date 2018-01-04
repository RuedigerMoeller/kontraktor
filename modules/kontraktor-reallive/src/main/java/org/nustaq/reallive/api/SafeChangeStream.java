package org.nustaq.reallive.api;


import org.nustaq.reallive.query.QParseException;

public interface SafeChangeStream {

    Subscriber subscribeOn(String query, ChangeReceiver receiver) throws QParseException;

}
