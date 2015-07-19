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

/**
 * Created by ruedi on 09.06.2015.
 */
public class BldResPath {

    transient BldFourK cfg4k;
    String urlPath = "/dyn";
    String resourcePath[];
    boolean cacheAggregates = true;
    Boolean compress;

    boolean inlineCss = true;
    boolean inlineScripts = true;
    boolean stripComments = true;
    boolean inlineHtml = true;
    boolean minify = true;

    public BldResPath(BldFourK cfg4k, String urlPath) {
        this.cfg4k = cfg4k;
        this.urlPath = urlPath;
    }

    public BldResPath elements(String... resourcePath) {
        this.resourcePath = resourcePath;
        return this;
    }

    public BldResPath inlineCss(final boolean inlineCss) {
        this.inlineCss = inlineCss;
        return this;
    }

    public BldResPath inlineHtml(final boolean inlineHtml) {
        this.inlineHtml = inlineHtml;
        return this;
    }

    public boolean isInlineHtml() {
        return inlineHtml;
    }

    public BldResPath inlineScripts(final boolean inlineScripts) {
        this.inlineScripts = inlineScripts;
        return this;
    }

    public BldResPath stripComments(final boolean stripComments) {
        this.stripComments = stripComments;
        return this;
    }

    public BldResPath minify(final boolean minify) {
        this.minify = minify;
        return this;
    }

    public boolean isInlineCss() {
        return inlineCss;
    }

    public boolean isInlineScripts() {
        return inlineScripts;
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

}
