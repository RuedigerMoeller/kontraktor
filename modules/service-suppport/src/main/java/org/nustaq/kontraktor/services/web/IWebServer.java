package org.nustaq.kontraktor.services.web;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.remoting.http.HttpSyncActorAdaptorHandler;
import org.nustaq.kontraktor.util.Log;

import java.net.URLDecoder;

/**
 * Created by ruedi on 29.05.17.
 */
public interface IWebServer {

    default void handleDirectRequest(HttpServerExchange exchange) {
        Log.Info(this,"direct request received "+exchange);
        exchange.setResponseCode(200);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html; charset=utf-8");
        exchange.getResponseSender().send(getResponse(exchange));
        exchange.endExchange();
    }

    @CallerSideMethod
    default String getResponse(HttpServerExchange exchange) {
//        String requestPath = URLDecoder.decode(requestPath,"UTF-8");
//        String[] tokens = requestPath.split("/");
        return "dummy response";
    }

}
