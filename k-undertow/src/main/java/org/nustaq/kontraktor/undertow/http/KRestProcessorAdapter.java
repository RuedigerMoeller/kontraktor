package org.nustaq.kontraktor.undertow.http;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.nustaq.kontraktor.remoting.http.KontraktorHttpRequest;
import org.nustaq.kontraktor.remoting.http.RequestResponse;
import org.nustaq.kontraktor.remoting.http.RestActorServer;
import org.nustaq.kontraktor.remoting.http.RestProcessor;
import org.nustaq.kontraktor.util.Log;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.channels.StreamSourceChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by ruedi on 04.04.2015.
 *
 * decodes html request into actor calls and routes back results of promises and callbacks
 */
public class KRestProcessorAdapter implements HttpHandler {

    RestProcessor rp;

    public KRestProcessorAdapter(RestProcessor rp) {
        this.rp = rp;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.dispatch();
        AtomicReference<StreamSinkChannel> responseChannel = new AtomicReference<>();
        KUTReq req = new KUTReq(exchange);
        if ( req.isPOST() ) {
            StreamSourceChannel requestChannel = exchange.getRequestChannel();
            String first = exchange.getRequestHeaders().getFirst(Headers.CONTENT_LENGTH);
            int len = Integer.parseInt(first);
            ByteBuffer buf = ByteBuffer.allocate(len);
            while ( buf.remaining() > 0 ) {
                if ( requestChannel.read(buf) < 0 ) {
                    throw new RuntimeException("failed to read "+len+" bytes from request");
                }
            }
            req.setContent( new String(buf.array(), "UTF-8") );
        }
        rp.processRequest(req, (resp,e) -> {
            if ( resp != null ) {
                if ( resp == RequestResponse.MSG_200 ) {
                    exchange.setResponseCode(200);
                    exchange.getResponseHeaders().add( new HttpString("Access-Control-Allow-Origin"), "*" );
                    aquireChannel(exchange, responseChannel);
                } else if ( resp.getStatusCode() == 302 ) { // redirect
                    exchange.setResponseCode(302);
                    exchange.getResponseHeaders().add(Headers.LOCATION, resp.getLocation());
                    aquireChannel(exchange, responseChannel);
                } else if ( resp == RequestResponse.MSG_403 ) {
                    exchange.setResponseCode(403);
                    aquireChannel(exchange, responseChannel);
                } else if ( resp == RequestResponse.MSG_404 ) {
                    exchange.setResponseCode(404);
                    aquireChannel(exchange, responseChannel);
                }else if ( resp == RequestResponse.MSG_500 ) {
                    exchange.setResponseCode(500);
                    aquireChannel(exchange, responseChannel);
                } else {
                    try {
                        writeBlocking(exchange, responseChannel.get(), resp);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        Log.Lg.warnLong(this,e1,"write http response");
                        exchange.endExchange();
                        return;
                    }
                }
            }
            if ( e == RestActorServer.FINISHED )
                exchange.endExchange();
        });
    }

    protected void aquireChannel(HttpServerExchange exchange, AtomicReference<StreamSinkChannel> responseChannel) {
        if ( responseChannel.get() != null ) {
            Log.Warn(this, "response already started, ignoring sent header");
        }
        responseChannel.set(exchange.getResponseChannel());
    }

    protected void writeBlocking(HttpServerExchange exchange, StreamSinkChannel responseChannel, RequestResponse resp) throws IOException {
        byte[] binary = resp.getBinary();
        ByteBuffer wrap = ByteBuffer.wrap(binary);
        while( wrap.remaining() > 0 )
            responseChannel.write(wrap);
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

        public void setContent(String content) {
            this.content = content;
        }

        @Override
        public CharSequence getText(){
            return content;
        }

        @Override
        public String getAccept() {
            return ex.getRequestHeaders().get(Headers.ACCEPT).getFirst();
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
