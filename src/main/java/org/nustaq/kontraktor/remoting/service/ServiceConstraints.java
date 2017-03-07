package org.nustaq.kontraktor.remoting.service;

import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;

/**
 * Created by ruedi on 06.03.17.
 */
public interface ServiceConstraints {

    // register a new session token (~connect)
    DenialReason registerToken(String token, String uname);
    // verify if remote call is allowed and within SLAs
    DenialReason isCallValid(Object token, RemoteCallEntry rce);

}
