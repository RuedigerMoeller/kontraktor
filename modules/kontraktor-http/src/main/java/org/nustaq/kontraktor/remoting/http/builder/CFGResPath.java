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

import org.nustaq.kontraktor.remoting.http.javascript.DynamicResourceManager;

import java.io.File;

/**
 * Created by ruedi on 09.06.2015.
 */
public class CFGResPath {

    CFGFourK cfg4k;
    String urlPath = "/dyn";
    String resourcePath[];
    String rootComponent = "app";
    boolean devMode = true;
    Boolean compress;

    HtmlImportShimSettings imports;

    // html import settings
    public static class HtmlImportShimSettings {
        boolean inlineCss = true;
        boolean inlineScripts = true;
        boolean stripComments = true;
        boolean minify = true;
        CFGResPath parent;

        public HtmlImportShimSettings(CFGResPath parent) {
            this.parent = parent;
        }

        public HtmlImportShimSettings inlineCss(final boolean inlineCss) {
            this.inlineCss = inlineCss;
            return this;
        }

        public HtmlImportShimSettings inlineScripts(final boolean inlineScripts) {
            this.inlineScripts = inlineScripts;
            return this;
        }

        public HtmlImportShimSettings stripComments(final boolean stripComments) {
            this.stripComments = stripComments;
            return this;
        }

        public HtmlImportShimSettings minify(final boolean minify) {
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

        public CFGResPath build() {
            return parent;
        }
    }


    public CFGResPath(CFGFourK cfg4k, String urlPath) {
        this.cfg4k = cfg4k;
        this.urlPath = urlPath;
    }

    public CFGResPath resourcePath(String ... resourcePath) {
        this.resourcePath = resourcePath;
        return this;
    }

    /**
     * calling this implicitely enables server side HtmlImport Polyfill
     * @return
     */
    public HtmlImportShimSettings imports() {
        if ( imports == null ) {
            imports = new HtmlImportShimSettings(this);
        }
        return imports;
    }

    public CFGResPath rootComponent(String rootComponent) {
        this.rootComponent = rootComponent;
        return this;
    }

    public CFGResPath devMode(boolean devMode) {
        this.devMode = devMode;
        return this;
    }

    public CFGResPath compress(boolean doGZip) {
        compress = doGZip;
        return this;
    }

    public CFGFourK build() {
        return cfg4k;
    }

    public String getUrlPath() {
        return urlPath;
    }

    public boolean isDevMode() {
        return devMode;
    }

    public String getRootComponent() {
        return rootComponent;
    }

    public String[] getResourcePath() {
        return resourcePath;
    }

    public boolean isCompress() {
        if (compress == null)
            return ! devMode;
        return compress;
    }

    public HtmlImportShimSettings getImports() {
        return imports;
    }

}
