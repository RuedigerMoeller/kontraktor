/*
Kontraktor-Http Copyright (c) Ruediger Moeller, All rights reserved.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3.0 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

See https://www.gnu.org/licenses/lgpl.txt
*/

package org.nustaq.kontraktor.remoting.http.undertow;

import io.undertow.Undertow;
import io.undertow.io.Sender;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.cache.CacheHandler;
import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.*;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.http.undertow.builder.BldFourK;
import org.nustaq.kontraktor.webapp.javascript.DynamicResourceManager;
import org.nustaq.kontraktor.util.Pair;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.GZIPOutputStream;

/**
 * Created by ruedi on 25/05/15.
 *
 * singleton to manage http server instances. Currently tied to Undertow however implicitely shields kontraktor-http
 * from getting too dependent on Undertow (which is an excellent piece of software, so no plans to migrate anytime soon)
 *
 */
public class Http4K {

    public static int UNDERTOW_IO_THREADS = 8;
    public static int UNDERTOW_WORKER_THREADS = 8;
    protected static Http4K instance;
    public static void set(Http4K http) {
        instance = http;
    }

    public static Http4K get() {
        synchronized (Http4K.class) {
            if ( instance == null ) {
                System.setProperty("org.jboss.logging.provider","slf4j");
                instance = new Http4K();
            }
            return instance;
        }
    }

    public static BldFourK Build( String hostName, int port, SSLContext ctx ) {
        return get().builder(hostName,port,ctx);
    }

    public static BldFourK Build( String hostName, int port) {
        return get().builder(hostName, port, null);
    }

    // a map of port=>server
    protected Map<Integer, Pair<PathHandler,Undertow>> serverMap = new HashMap<>();

    /**
     * creates or gets an undertow web server instance mapped by port.
     * hostname must be given in case a new server instance has to be instantiated
     *
     * @param port
     * @param hostName
     * @return
     */
    public synchronized Pair<PathHandler, Undertow> getServer(int port, String hostName) {
        return getServer(port,hostName,null);
    }

    public synchronized Pair<PathHandler, Undertow> getServer(int port, String hostName, SSLContext context) {
        Pair<PathHandler, Undertow> pair = serverMap.get(port);
        if (pair == null) {
            PathHandler pathHandler = new PathHandler();
            Undertow.Builder builder = Undertow.builder()
                                           .setIoThreads(UNDERTOW_IO_THREADS)
                                           .setWorkerThreads(UNDERTOW_WORKER_THREADS);
            Undertow server = customize(builder,pathHandler,port,hostName,context).build();
            server.start();
            pair = new Pair<>(pathHandler,server);
            serverMap.put(port,pair);
        }
        return pair;
    }

    public BldFourK builder(String hostName, int port, SSLContext ctx) {
        return new BldFourK(hostName,port,ctx);
    }

    public BldFourK builder(String hostName, int port) {
        return new BldFourK(hostName,port,null);
    }

    protected Undertow.Builder customize(Undertow.Builder builder, PathHandler rootPathHandler, int port, String hostName, SSLContext context) {
        if ( context == null ) {
            return builder
                       .addHttpListener(port, hostName)
                       .setHandler(rootPathHandler);
        } else {
            return builder
                       .addHttpsListener(port,hostName,context)
                       .setHandler(rootPathHandler);
        }
    }

    /**
     * publishes given file root
     * @param hostName
     * @param urlPath - prefixPath (e.g. /myapp/resource)
     * @param port
     * @param root - directory to be published
     */
    public Http4K publishFileSystem( String hostName, String urlPath, int port, File root ) {
        if ( ! root.isDirectory() ) {
            throw new RuntimeException("root must be an existing direcory:"+root.getAbsolutePath());
        }
        Pair<PathHandler, Undertow> server = getServer(port, hostName);
        server.car().addPrefixPath(urlPath, new ResourceHandler(new FileResourceManager(root,100)));
        return this;
    }

    // FIXME: exposes Undertow class
    public Http4K publishFileSystem( String hostName, String urlPath, int port, FileResourceManager man ) {
        if ( ! man.getBase().isDirectory() ) {
            throw new RuntimeException("root must be an existing direcory:"+man.getBase().getAbsolutePath());
        }
        Pair<PathHandler, Undertow> server = getServer(port, hostName);
        server.car().addPrefixPath(urlPath, new ResourceHandler(man ));
        return this;
    }
    public Http4K publishResourcePath( String hostName, String urlPath, int port, DynamicResourceManager man, boolean compress ) {
        return publishResourcePath(hostName,urlPath,port,man,compress,null);
    }

    // only called once in case, no need for optimization
    static byte[] gzip(byte[] val) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(val.length);
        GZIPOutputStream gos = null;
        try {
            gos = new GZIPOutputStream(bos);
            gos.write(val, 0, val.length);
            gos.finish();
            gos.flush();
            bos.flush();
            val = bos.toByteArray();
        } finally {
            if (gos != null)
                gos.close();
            if (bos != null)
                bos.close();
        }
        return val;
    }

    public Http4K publishResourcePath(String hostName, String urlPath, int port, DynamicResourceManager man, boolean compress, Function<HttpServerExchange,Boolean> interceptor ) {
        Pair<PathHandler, Undertow> server = getServer(port, hostName);
        ResourceHandler handler = new ResourceHandler(man);
        if ( compress ) {
            HttpHandler compressHandler = new EncodingHandler.Builder().build(new HashMap<>()).wrap(handler);
            HttpHandler httpHandler = new HttpHandler() {
                volatile byte[] zippedAggregate;
                @Override
                public void handleRequest(HttpServerExchange exchange) throws Exception {
                    String requestPath = exchange.getRequestPath();
                    if ( exchange.getRequestMethod() == Methods.GET && !man.isDevMode() &&
                        ( requestPath.equals("/") || requestPath.equals("") || requestPath.equals("/index.html") )
                    ) {
                        HeaderMap requestHeaders = exchange.getRequestHeaders();
                        String lastMod = requestHeaders.get(Headers.IF_MODIFIED_SINCE,0);
                        if ( lastMod != null ) {
                            Date date = DateUtils.parseDate(lastMod);
                            if ( date != null && date.getTime() >= man.getLastModified()-60_000) {
                                exchange.setResponseCode(304);
                                exchange.endExchange();
                                return;
                            }
                        }
                        Resource cacheEntry = man.getCacheEntry("index.html");
                        if ( cacheEntry instanceof DynamicResourceManager.MyResource ) {
                            if ( zippedAggregate == null ) {
                                zippedAggregate = gzip(((DynamicResourceManager.MyResource) cacheEntry).getBytes());
                            }
                            exchange.setResponseCode(200);
                            exchange.getResponseHeaders().put(Headers.CONTENT_ENCODING,"gzip");
                            exchange.getResponseHeaders().put(Headers.LAST_MODIFIED,DateUtils.toDateString(man.getLastModifiedDate()));
                            Sender responseSender = exchange.getResponseSender();
                            responseSender.send(ByteBuffer.wrap(zippedAggregate));
                        } else {
                            zippedAggregate = null;
                            compressHandler.handleRequest(exchange);
                        }
                    } else {
                        handler.handleRequest(exchange);
                    }
                }
            };
            if ( interceptor != null ) {
                server.car().addPrefixPath( urlPath, httpExchange -> {
                    boolean apply = interceptor.apply(httpExchange);
                    if ( ! apply ) {
                        httpHandler.handleRequest(httpExchange);
                    }
                });
            } else {
                server.car().addPrefixPath( urlPath, httpHandler);
            }
        } else {
            if ( interceptor != null ) {
                server.car().addPrefixPath( urlPath, httpExchange -> {
                    boolean apply = interceptor.apply(httpExchange);
                    if ( ! apply ) {
                        handler.handleRequest(httpExchange);
                    }
                });
            } else {
                server.car().addPrefixPath( urlPath, handler);
            }
        }
        return this;
    }

    /**
     * utility, just redirects to approriate connector
     *
     * Publishes an actor/service via websockets protocol with given encoding.
     * if this should be connectable from non-java code recommended coding is 'new Coding(SerializerType.JsonNoRefPretty)' (dev),
     * 'new Coding(SerializerType.JsonNoRef)' (production)
     *
     * SerializerType.FSTSer is the most effective for java to java communication.
     *
     */
    public IPromise<ActorServer> publish( WebSocketPublisher publisher ) {
        return publisher.publish();
    }

    /**
     * utility, just redirects to approriate connector.
     */
    public IPromise<ActorServer> publish( HttpPublisher publisher ) {
        return publisher.publish();
    }

    public Http4K publishHandler(String hostName, String urlPath, int port, HttpHandler handler) {
        Pair<PathHandler, Undertow> server = getServer(port, hostName);
        server.car().addPrefixPath( urlPath, handler);
        return this;
    }

    public Http4K unPublishHandler(String urlPath, int port) {
        Pair<PathHandler, Undertow> server = serverMap.get(port);
        if (server!=null) {
            server.car().removePrefixPath(urlPath);
        }
        return this;
    }
}
