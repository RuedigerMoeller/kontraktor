package org.nustaq.kontraktor.barebone;

/**
 * Created by ruedi on 05/06/15.
 *
 * Mirrors Kontraktor's RemoteCallEntry
 */
public class BBRemoteCallEntry {

    int receiverKey; // id of published actor in host, contains cbId in case of callbacks
    int futureKey; // id of future if any
    String method;
    Object args[];
    int queue;

}
