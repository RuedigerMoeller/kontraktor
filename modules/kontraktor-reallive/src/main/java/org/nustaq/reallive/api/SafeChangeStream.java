package org.nustaq.reallive.api;

import java.text.ParseException;

public interface SafeChangeStream {

    Subscriber subscribeOn(String query, ChangeReceiver receiver) throws ParseException;

}
