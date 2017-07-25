package org.nustaq.reallive.interfaces;

import org.nustaq.kontraktor.IPromise;

/**
 * Created by ruedi on 07/08/15.
 */
public interface AsyncKV {

    IPromise<Record> get(String key );
    IPromise<Long> size();

}
