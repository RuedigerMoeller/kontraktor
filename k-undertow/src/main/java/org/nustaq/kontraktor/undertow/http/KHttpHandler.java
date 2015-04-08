package org.nustaq.kontraktor.undertow.http;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.nustaq.kontraktor.Actor;

/**
 * Created by ruedi on 27/03/15.
 *
 * A httphandler enabling to hook kontraktors async actor/event style into undertow
 *
 */
public class KHttpHandler<T extends KHttpHandler> extends Actor<T> implements HttpHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.dispatch();
        self().$handleRequest(exchange);
    }

    public void $handleRequest(HttpServerExchange exchange) {
        exchange.endExchange();
    }

}
