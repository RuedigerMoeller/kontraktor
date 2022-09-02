package org.nustaq.kontraktor.remoting.http.undertow;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.nustaq.kontraktor.remoting.http.KHttpExchange;
import org.nustaq.kontraktor.util.Log;
import org.xnio.channels.StreamSinkChannel;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by ruedi on 19.06.17.
 */
public class UndertowKHttpExchangeImpl implements KHttpExchange {

    final HttpServerExchange ex;

    public UndertowKHttpExchangeImpl(HttpServerExchange ex) {
        this.ex = ex;
    }

    @Override
    public void endExchange() {
        ex.endExchange();
    }

    @Override
    public void setResponseContentLength(int length) {
        ex.setResponseContentLength(length);
    }

    @Override
    public void setResponseCode(int i) {
        ex.setResponseCode(i);
    }

    @Override
    public void send(String s) {
        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html; charset=utf-8");
        ex.getResponseSender().send( s );
    }

    @Override
    public void send(byte [] b) {
        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html; charset=utf-8");
        ex.getResponseSender().send( ByteBuffer.wrap(b) );
    }

    @Override
    public void sendAuthResponse(byte[] response, String sessionId) {
        ByteBuffer responseBuf = ByteBuffer.wrap(response);
        ex.setResponseCode(200);
        ex.setResponseContentLength(response.length);
        StreamSinkChannel sinkchannel = ex.getResponseChannel();
        String finalSessionId1 = sessionId;
        sinkchannel.getWriteSetter().set(
            channel -> {
                if ( responseBuf.remaining() > 0 )
                    try {
                        sinkchannel.write(responseBuf);
                        if (responseBuf.remaining() == 0) {
                            Log.Info(this, "client connected " + finalSessionId1);
                            ex.endExchange();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        ex.endExchange();
                    }
                else
                {
                    Log.Info(this,"client connected "+ finalSessionId1);
                    ex.endExchange();
                }
            }
        );
        sinkchannel.resumeWrites();
    }

    @Override
    public String getPath() {
        return ex.getRequestPath();
    }
}
