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
package org.nustaq.kontraktor.remoting.http.builder;

import io.undertow.server.HttpServerExchange;
import org.nustaq.kontraktor.remoting.http.javascript.TranspilerHook;

import javax.xml.ws.spi.http.HttpExchange;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by ruedi on 09.06.2015.
 */
public class BldResPath {

    transient BldFourK cfg4k;
    String urlPath = "/dyn";
    String resourcePath[];
    String baseDir =".";
    boolean cacheAggregates = true;
    Boolean compress;

    boolean inline = true;
    boolean stripComments = true;
    boolean minify = true;
    transient Map<String,TranspilerHook> transpilers = new HashMap<>();
    // all requests are forwarded to this, return true in case function wants to capture the request
    Function<HttpServerExchange, Boolean> handlerInterceptor;

    public BldResPath(BldFourK cfg4k, String urlPath) {
        this.cfg4k = cfg4k;
        this.urlPath = urlPath;
    }
    public BldResPath handlerInterceptor(final Function<HttpServerExchange,Boolean> handlerInterceptor) {
        this.handlerInterceptor = handlerInterceptor;
        return this;
    }

    public Function<HttpServerExchange, Boolean> getHandlerInterceptor() {
        return handlerInterceptor;
    }

    /**
     * @param resourcePath - a list of directories to lookup when searching for imports/resources
     * @return
     */
    public BldResPath elements(String... resourcePath) {
        this.resourcePath = resourcePath;
        return this;
    }

    public BldResPath inline(final boolean inline) {
        this.inline = inline;
        return this;
    }

    public BldResPath stripComments(final boolean stripComments) {
        this.stripComments = stripComments;
        return this;
    }

    public BldResPath baseDir(final String baseDir) {
        this.baseDir = baseDir;
        return this;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public BldResPath minify(final boolean minify) {
        this.minify = minify;
        return this;
    }

    public boolean isInline() {
        return inline;
    }

    public boolean isStripComments() {
        return stripComments;
    }

    public boolean isMinify() {
        return minify;
    }

    public BldResPath cacheAggregates(boolean cacheAggregates) {
        this.cacheAggregates = cacheAggregates;
        return this;
    }

    /**
     * turn on/off all inlining + file caching for easy development.
     * Note for full dev mode a html import enabled browser is required (e.g. chrome)
     *
     * note this overwrites previous settings made to this
     */
    public BldResPath allDev(boolean dev) {
        inline(!dev);
        stripComments(!dev);
        minify(!dev);
        cacheAggregates(!dev);
        compress(!dev);
        return this;
    }

    public BldResPath compress(boolean doGZip) {
        compress = doGZip;
        return this;
    }

    public BldFourK build() {
        return cfg4k;
    }

    public String getUrlPath() {
        return urlPath;
    }

    public boolean isCacheAggregates() {
        return cacheAggregates;
    }

    public String[] getResourcePath() {
        return resourcePath;
    }

    public boolean isCompress() {
        if (compress == null)
            return !cacheAggregates;
        return compress;
    }

    public BldResPath transpile( String ending, TranspilerHook hook ) {
        transpilers.put(ending,hook);
        return this;
    }

    public Map<String, TranspilerHook> getTranspilers() {
        return transpilers;
    }
}
