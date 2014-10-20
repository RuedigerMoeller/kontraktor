package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.Callback;

/**
 * Created by ruedi on 13.08.2014.
 *
 * processes http (rest) requests, can be asynchronous (e.g. implemented by an actor)
 *
 */
public interface RequestProcessor {

    public static String FINISHED = "FIN"; // must be sent once all responses are sent
    public boolean processRequest(KontraktorHttpRequest req, Callback<RequestResponse> response);

}
