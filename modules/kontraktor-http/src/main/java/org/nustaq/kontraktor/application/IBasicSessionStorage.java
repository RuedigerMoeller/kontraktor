package org.nustaq.kontraktor.application;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;

/**
 * Created by ruedi on 20.06.17.
 */
public interface IBasicSessionStorage {

    IPromise<String> getUserFromSessionId(String user);

}
