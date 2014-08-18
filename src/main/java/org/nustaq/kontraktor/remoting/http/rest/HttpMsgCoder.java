package org.nustaq.kontraktor.remoting.http.rest;

import org.nustaq.kontraktor.remoting.RemoteCallEntry;
import org.nustaq.kontraktor.remoting.http.KontraktorHttpRequest;
import org.nustaq.kontraktor.remoting.http.KontraktorHttpRequestImpl;

/**
 * Created by ruedi on 17.08.14.
 */
public interface HttpMsgCoder {

    RemoteCallEntry [] decodeFrom( String s, KontraktorHttpRequest req ) throws Exception;
    String encode( RemoteCallEntry resultOrCb ) throws Exception;

}
