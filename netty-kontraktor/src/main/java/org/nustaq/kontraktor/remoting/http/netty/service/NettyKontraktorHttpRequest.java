package org.nustaq.kontraktor.remoting.http.netty.service;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.nustaq.kontraktor.remoting.http.KontraktorHttpRequest;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by ruedi on 18.08.14.
 *
 * note: this is a "works" implementation. However remoting via http is slow anyway
 */
public class NettyKontraktorHttpRequest implements KontraktorHttpRequest {

    FullHttpRequest req;
    String content;
    private String[] splitPath;

    public NettyKontraktorHttpRequest(FullHttpRequest req) {
        this.req = req;
    }

    @Override
    public boolean isGET() {
        return req.getMethod() == HttpMethod.GET;
    }

    @Override
    public String getPath(int i) {
        if (splitPath == null ) {
            splitPath = req.getUri().split("/");
        }
        if ( i+1 < splitPath.length ) {
            return splitPath[i+1];
        }
        return "";
    }

    @Override
    public boolean isPOST() {
        return req.getMethod() == HttpMethod.POST;
    }

    @Override
    public CharSequence getText() {
        if ( content == null )
            content = req.content().toString(Charset.forName("UTF-8"));
        return content;
    }

    @Override
    public String getAccept() {
        return req.headers().get(HttpHeaders.Names.ACCEPT);
    }

    @Override
    public void append(ByteBuffer buffer, int bytesread) {
        // already assembled by netty
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
