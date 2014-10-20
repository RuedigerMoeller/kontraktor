package org.nustaq.kontraktor.remoting.http;

import javassist.bytecode.Descriptor;

import java.nio.ByteBuffer;

/**
 * Created by ruedi on 18.08.14.
 *
 * Stripped down interface of a http request, as kontraktor http remoting requires only a fraction of http.
 */
public interface KontraktorHttpRequest {

    boolean isGET();
    // return i'th element of path. if at end return ""
    String getPath(int i);
    boolean isPOST();
    CharSequence getText();
    String getAccept();
    void append(ByteBuffer buffer, int bytesread);
    boolean isComplete();

    default String getFullPath() {
        StringBuilder res = new StringBuilder(64);
        for ( int i = 0; ! getPath(i).equals(""); i++ ) {
            res.append('/');
            res.append(getPath(i));
        }
        return res.toString();
    }

}
