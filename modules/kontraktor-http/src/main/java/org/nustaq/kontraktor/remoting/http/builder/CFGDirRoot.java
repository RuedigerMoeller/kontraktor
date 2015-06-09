package org.nustaq.kontraktor.remoting.http.builder;

/**
 * Created by ruedi on 09.06.2015.
 */
public class CFGDirRoot {

    String urlPath;
    String dir;

    public CFGDirRoot(String urlPath, String dir) {
        this.dir = dir;
        this.urlPath = urlPath;
    }

    public String getUrlPath() {
        return this.urlPath;
    }

    public String getDir() {
        return this.dir;
    }

    public CFGDirRoot urlPath(final String urlPath) {
        this.urlPath = urlPath;
        return this;
    }

    public CFGDirRoot dir(final String dir) {
        this.dir = dir;
        return this;
    }


}
