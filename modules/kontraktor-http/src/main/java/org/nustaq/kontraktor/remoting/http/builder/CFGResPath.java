package org.nustaq.kontraktor.remoting.http.builder;

import org.nustaq.kontraktor.remoting.http.javascript.DynamicResourceManager;

/**
 * Created by ruedi on 09.06.2015.
 */
public class CFGResPath {

    CFGFourK cfg4k;
    String urlPath = "/dyn";
    String resourcePath[];
    String rootComponent = "app";
    boolean devMode = true;

    public CFGResPath(CFGFourK cfg4k, String urlPath) {
        this.cfg4k = cfg4k;
        this.urlPath = urlPath;
    }

    public CFGResPath resourcePath(String ... resourcePath) {
        this.resourcePath = resourcePath;
        return this;
    }

    public CFGResPath rootComponent(String rootComponent) {
        this.rootComponent = rootComponent;
        return this;
    }

    public CFGResPath devMode(boolean devMode) {
        this.devMode = devMode;
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
}
