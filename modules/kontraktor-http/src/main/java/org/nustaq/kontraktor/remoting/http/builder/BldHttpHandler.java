package org.nustaq.kontraktor.remoting.http.builder;

import io.undertow.server.HttpHandler;

/**
 * Created by ruedi on 02.09.2015.
 */
public class BldHttpHandler {

    String urlPath;
    HttpHandler handler;

    public BldHttpHandler(String urlPath, HttpHandler handler) {
        this.urlPath = urlPath;
        this.handler = handler;
    }

    public String getUrlPath() {
        return urlPath;
    }

    public HttpHandler getHandler() {
        return handler;
    }
}
