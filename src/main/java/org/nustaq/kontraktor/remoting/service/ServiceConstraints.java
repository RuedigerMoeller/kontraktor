package org.nustaq.kontraktor.remoting.service;

import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;

/**
 * Created by ruedi on 06.03.17.
 */
public interface ServiceConstraints {

    // verify if remote call is allowed and within SLAs
    DenialReason isCallValid(Object token, RemoteCallEntry rce);

}
