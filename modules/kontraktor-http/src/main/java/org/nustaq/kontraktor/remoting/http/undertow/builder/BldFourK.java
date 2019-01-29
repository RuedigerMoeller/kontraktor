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
package org.nustaq.kontraktor.remoting.http.undertow.builder;

import io.undertow.server.HttpHandler;
import io.undertow.util.HeaderMap;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;
import org.nustaq.kontraktor.remoting.http.undertow.HttpPublisher;
import org.nustaq.kontraktor.rest.UndertowRESTHandler;
import org.nustaq.kontraktor.webapp.javascript.*;
import org.nustaq.kontraktor.remoting.http.undertow.WebSocketPublisher;
import org.nustaq.kontraktor.webapp.transpiler.jsx.FileWatcher;
import org.nustaq.serialization.util.FSTUtil;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by ruedi on 09.06.2015.
 */
public class BldFourK {

    String hostName;
    int port;
    SSLContext context;
    boolean httpCachedEnabled = false ;

    List items = new ArrayList<>();

    public BldFourK(String hostName, int port, SSLContext context) {
        this.hostName = hostName;
        this.port = port;
        this.context = context;
    }

    public BldFourK auto( Object serverAppActor, Object ... interfaceClasses ) {
        try {
            for (int i = 0; i < interfaceClasses.length; i++) {
                Object interfaceClass = interfaceClasses[i];
                Method auto = ((Class) interfaceClass).getMethod("auto", BldFourK.class, Object.class );
                auto.invoke(interfaceClass, this);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            FSTUtil.rethrow(e);
        }
        return this;
    }

    public BldFourK auto( AutoConfig serverAppActor ) {
        serverAppActor.auto(this);
        return this;
    }

    public BldFourK fileRoot(String urlPath, String dir) {
        BldDirRoot rt = new BldDirRoot(urlPath,dir);
        items.add(rt);
        return this;
    }

    public BldFourK httpCachedEnabled(boolean b)
    {
        httpCachedEnabled = b;
        return this;
    }

    public BldFourK fileRoot(String urlPath, File dir) {
        BldDirRoot rt = new BldDirRoot(urlPath,dir.getAbsolutePath());
        items.add(rt);
        return this;
    }

    public BldResPath resourcePath(String urlPath) {
        BldResPath rt = new BldResPath(this,urlPath);
        items.add(rt);
        return rt;
    }

    public WebSocketPublisher websocket( String urlPath, Actor facade ) {
        return websocket(urlPath,facade,false);
    }

    public BldFourK hmrServer(boolean on) {
        if ( on )
            return websocket("/hotreloading", FileWatcher.get())
                .serType(SerializerType.JsonNoRef)
                .buildWebsocket();
        return this;
    }

    public WebSocketPublisher websocket( String urlPath, Actor facade, boolean useStringMessages ) {
        WebSocketPublisher wp = new WebSocketPublisher(this,facade, hostName, urlPath, port);
        wp.sendStringMessages(useStringMessages);
        items.add(wp);
        return wp;
    }

    public BldFourK httpHandler( String urlPath, HttpHandler handler ) {
        items.add(new BldHttpHandler(urlPath,handler));
        return this;
    }

    public BldFourK restAPI( String urlPath, Actor restActor ) {
        items.add(new BldHttpHandler(urlPath,new UndertowRESTHandler(urlPath,restActor,null)));
        return this;
    }

    public BldFourK restAPI(String urlPath, Actor restActor, Function<HeaderMap, IPromise> reqauth) {
        items.add(new BldHttpHandler(urlPath,new UndertowRESTHandler(urlPath,restActor,reqauth)));
        return this;
    }

    public HttpPublisher httpAPI(String urlPath, Actor facade) {
        HttpPublisher hp = new HttpPublisher( this, facade, hostName, urlPath, port);
        items.add(hp);
        return hp;
    }

    public boolean getHttpCacheEnabled() { return httpCachedEnabled; }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    public SSLContext getContext() {
        return context;
    }

    public List getItems() {
        return items;
    }

    public BldFourK build() {
        Http4K http4K = Http4K.get();
        http4K.getServer(getPort(), getHostName(), context );//fixme https
        getItems().forEach(item -> {
            if (item instanceof HttpPublisher) {
                http4K.publish((HttpPublisher) item);
            } else if (item instanceof WebSocketPublisher) {
                http4K.publish((WebSocketPublisher) item);
            } else if (item instanceof BldDirRoot) {
                BldDirRoot dr = (BldDirRoot) item;
                if ( getHttpCacheEnabled() ) {
                    CachedFileResourceManager resMan = new CachedFileResourceManager( getHttpCacheEnabled() ,  new File(dr.getDir()) , 100 );
                    http4K.publishFileSystem(getHostName(), dr.getUrlPath(), getPort(), resMan );
                } else {
                    http4K.publishFileSystem(getHostName(), dr.getUrlPath(), getPort(), new File(dr.getDir()));
                }
            } else if (item instanceof BldHttpHandler) {
                BldHttpHandler dr = (BldHttpHandler) item;
                http4K.publishHandler( getHostName(), dr.getUrlPath(), getPort(), dr.getHandler());
            } else if (item instanceof BldResPath) {
                BldResPath dr = (BldResPath) item;
                DynamicResourceManager drm = new DynamicResourceManager(!dr.isCacheAggregates(), dr.getUrlPath(), dr.isMinify(), dr.getBaseDir(), dr.getResourcePath());
                drm.cachedIndexDir(dr.getProductionBuildDir());
                HtmlImportShim shim = new HtmlImportShim(dr.getUrlPath());
                drm.setJSPostProcessors(dr.getProdModePostProcessors());
                shim.setJSPostProcessors(dr.getProdModePostProcessors());
                shim
                    .minify(dr.isMinify())
                    .inline(dr.isInline())
                    .stripComments(dr.isStripComments());
                drm.setImportShim(shim);
                drm.setTranspilerMap(dr.getTranspilers());
                http4K.publishResourcePath( getHostName(), dr.getUrlPath(), getPort(), drm, dr.isCompress(), dr.getHandlerInterceptor() );
            } else {
                System.out.println("unexpected item " + item);
            }
        });
        return this;
    }

}
