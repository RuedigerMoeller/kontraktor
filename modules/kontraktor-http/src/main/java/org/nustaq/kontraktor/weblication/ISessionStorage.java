package org.nustaq.kontraktor.weblication;

import org.nustaq.kontraktor.IPromise;

/**
 * Created by ruedi on 20.06.17.
 */
public interface ISessionStorage {

    IPromise<String> getUserFromSessionId(String sid);

}
