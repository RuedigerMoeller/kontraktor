package org.nustaq.kontraktor.wapi;

/**
 * Created by ruedi on 07.03.17.
 */
public interface CallCounter {
    void countCall( String user, String methodName, String apiID );
}
