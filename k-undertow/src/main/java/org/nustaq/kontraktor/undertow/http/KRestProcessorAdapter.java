package org.nustaq.kontraktor.undertow.http;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.nustaq.kontraktor.remoting.http.KontraktorHttpRequest;
import org.nustaq.kontraktor.remoting.http.RequestResponse;
import org.nustaq.kontraktor.remoting.http.RestActorServer;
import org.nustaq.kontraktor.remoting.http.RestProcessor;
import org.nustaq.kontraktor.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by ruedi on 04.04.2015.
 */
public class KRestProcessorAdapter implements HttpHandler {

    RestProcessor rp;

    public KRestProcessorAdapter(RestProcessor rp) {
        this.rp = rp;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.dispatch();
        rp.processRequest(new KUTReq(exchange), (resp,e) -> {
            System.out.println("RESP:"+resp+" "+e);
            if ( resp != null ) {
                try {
                    writeBlocking(exchange, resp);
                } catch (Exception e1) {
                    e1.printStackTrace();
                    Log.Lg.warnLong(this,e1,"write http response");
                    exchange.endExchange();
                    return;
                }
            }
            if ( e == RestActorServer.FINISHED )
                exchange.endExchange();
        });
    }

    protected void writeBlocking(HttpServerExchange exchange, RequestResponse resp) throws IOException {
        byte[] binary = resp.getBinary();
        ByteBuffer wrap = ByteBuffer.wrap(binary);
        while( wrap.remaining() > 0 )
            exchange.getResponseChannel().write(wrap);
    }

    static class KUTReq implements KontraktorHttpRequest {

        HttpServerExchange ex;
        String[] splitPath;
        String content;

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
                splitPath = ex.getRelativePath().split("/");
            }
            if ( i+1 < splitPath.length ) {
                return splitPath[i+1];
            }
            return "";
        }

        @Override
        public boolean isPOST() {
            return ex.getRequestMethod().equals(Methods.POST);
        }

        @Override
        public CharSequence getText(){
            return content;
        }

        @Override
        public String getAccept() {
            return ex.getRequestHeaders().get(Headers.ACCEPT).toString();
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

}
