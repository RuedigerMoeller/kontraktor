package org.nustaq.kontraktor.undertow.http;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.nustaq.kontraktor.remoting.http.KontraktorHttpRequest;
import org.nustaq.kontraktor.remoting.spa.FourK;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
* Created by ruedi on 14/04/15.
*/
public class KUTReq implements KontraktorHttpRequest {

    HttpServerExchange ex;
    String[] splitPath;
    String content;
    byte binaryContent[];

    public KUTReq(HttpServerExchange ex) {
        this.ex = ex;
    }

    @Override
    public boolean isGET() {
        return ex.getRequestMethod().equals(Methods.GET);
    }

    @Override
    public String getPath(int i) {
        if (splitPath == null ) {
            splitPath = FourK.StripDoubleSeps(ex.getRelativePath()).split("/");
        }
        if ( i+1 < splitPath.length ) {
            return splitPath[i+1];
        }
        return "";
    }

    @Override
    public int getPathLen() {
        if ( splitPath == null )
            getPath(0);
        return splitPath.length-1;
    }

    public byte[] getBinaryContent() {
        return binaryContent;
    }

    public void setBinaryContent(byte[] binaryContent) {
        this.binaryContent = binaryContent;
    }

    @Override
    public boolean isPOST() {
        return ex.getRequestMethod().equals(Methods.POST);
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public CharSequence getText(){
        return content;
    }

    @Override
    public byte[] getBinary() {
        return binaryContent;
    }

    @Override
    public String getAccept() {
        HeaderValues strings = ex.getRequestHeaders().get(Headers.ACCEPT);
        if ( strings == null )
            return null;
        return strings.getFirst();
    }

    // probably deprecated
    @Override
    public void append(ByteBuffer buffer, int bytesread) {
        throw new RuntimeException("not implemented");
    }

    // probably deprecated
    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public String toString() {
        getPath(0);
        try {
            return "KUTReq{" +
                    "ex=" + ex +
                    ", splitPath=" + Arrays.toString(splitPath) +
                    ", content='" + getText() + '\'' +
                    '}';
        } catch (Exception e) {
            return ""+e;
        }
    }
}
