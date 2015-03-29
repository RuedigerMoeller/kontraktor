package org.nustaq.kontraktor.remoting.http.websocket;

import org.nustaq.kontraktor.Scheduler;
import org.nustaq.kontraktor.impl.ElasticScheduler;
import org.nustaq.kontraktor.remoting.Coding;

/**
 * implements mechanics of transparent actor remoting via websockets based on the websocket api
 * inherited.
 *
 * FIXME: inherited Per-Client session actor is superfluous and adds latency per actor caused by redundant message enqueuing
 * FIXME: currently flow is httpserver =enqueue=> actor server =enq=> connection session =enq=> AppServerActor/AppSessionActor
 */
public class WebSocketEndpoint {

    // don't buffer too much messages (memory issues with many clients)
    public static int DEFAULT_CLIENTQ_SIZE = 1000;
    public static int DEFAULT_MAX_THREADS = 1;

    protected Scheduler connectionScheduler;

    public WebSocketEndpoint(Coding coding, int maxThreadsForAllConnections, int queueSizePerConnection) {
        this( new ElasticScheduler(maxThreadsForAllConnections, queueSizePerConnection) );
    }

    public WebSocketEndpoint(Scheduler connectionScheduler) {
        this.connectionScheduler = connectionScheduler;
    }

    public WebSocketEndpoint(Coding coding) {
        this( coding, DEFAULT_MAX_THREADS, DEFAULT_CLIENTQ_SIZE );
    }

    public void onBinaryMessage(WebSocketCchannelAdapter ctx, byte[] buffer) {
        System.out.println("onBinary");
    }

    public void onOpen(WebSocketCchannelAdapter ctx) {
        System.out.println("onOpen");
    }

    public void onClose(WebSocketCchannelAdapter ctx) {
        System.out.println("onClose");
    }

    public void onTextMessage(WebSocketCchannelAdapter ctx, String text) {
        System.out.println("onText:" + text);
    }

    public void onPong(WebSocketCchannelAdapter ctx) {
        System.out.println("onPong");
    }

    public void sendWSPingMessage(WebSocketCchannelAdapter tcx) {

    }

    public void sendWSBinaryMessage(WebSocketCchannelAdapter tcx, byte[] b) {

    }

    public void sendWSTextMessage(WebSocketCchannelAdapter tcx, String s) {

    }

    public void onError(WebSocketCchannelAdapter channel) {

    }
}
