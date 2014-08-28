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
    String getPath(int i);
    boolean isPOST();
    CharSequence getText();
    String getAccept();
    void append(ByteBuffer buffer, int bytesread);
    boolean isComplete();
}
