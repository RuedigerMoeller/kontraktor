package org.nustaq.kontraktor.undertow.javascript;

import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.FileResource;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.ETag;
import io.undertow.util.MimeMappings;
import org.nustaq.kontraktor.remoting.javascript.DependencyResolver;
import org.nustaq.kontraktor.undertow.Knode;
import org.nustaq.kontraktor.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;

/**
 * adapts kontraktors js + html snippets dependency management to undertow
 *
 */
public class DynamicResourceManager extends FileResourceManager {

    boolean devMode = false;
    DependencyResolver dependencyResolver;

    public DynamicResourceManager(boolean devMode, String root, String[] resourcePath) {
        super(new File("."), 100);
        this.devMode = devMode;
        dependencyResolver = new DependencyResolver(root,resourcePath);
        setBase(dependencyResolver.locateComponent(root).get(0));
    }

    public DynamicResourceManager(boolean devMode, String root, DependencyResolver dependencyResolver) {
        super(new File("."), 100);
        this.devMode = devMode;
        this.dependencyResolver = dependencyResolver;
        List<File> files = dependencyResolver.locateComponent(root);
        if ( files.size() == 0 ) {
            Log.Lg.error(this,null, "Fatal Error: unable to locate root component '"+root+"' on resourcepath.");
        }
        setBase(files.get(0));
    }

    public boolean isDevMode() {
        return devMode;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    @Override
    public Resource getResource(String p0) {
        String p = p0;
        if (p.startsWith("/")) {
            p = p.substring(1);
        }
        if ( p.startsWith("lookup/") ) {
            p = p.substring("lookup/".length());
            File file = dependencyResolver.locateResource(p);
            if ( file != null ) {
                return new FileResource(file, this, p0);
            } else {
                return null;
            }
        } else if ( p.startsWith("merged/") ) { // expect simple ending like '.js'
            p = p.substring("merged/".length());
            final String finalP = p;
            List<String> filesInDirs = dependencyResolver.findFilesInDirs(finam -> finam.endsWith(finalP));
            byte[] bytes = null;
            if ( finalP.endsWith(".css") ) {
                bytes = dependencyResolver.mergeBinary(filesInDirs);
            } else {
                bytes = dependencyResolver.mergeTextSnippets(filesInDirs,"","");
            }
            return new MyResource(p0, finalP, bytes);
        } else if (p.equals("scripts.js") ) { // expect simple ending like '.js'
            final String finalP = p;
            List<String> filesInDirs = dependencyResolver.findFilesInDirs(finam -> finam.endsWith(".js"));
            byte[] bytes = null;
            if ( devMode )
                bytes = dependencyResolver.createScriptTags(filesInDirs);
            else
                bytes = dependencyResolver.mergeScripts(filesInDirs);
            return new MyResource(p0, finalP, bytes);
        }
        return super.getResource(p0);
    }

    private static class MyResource implements Resource {
        private final String p0;
        private final String finalP;
        private final byte[] bytes;

        public MyResource(String p0, String finalP, byte[] bytes) {
            this.p0 = p0;
            this.finalP = finalP;
            this.bytes = bytes;
        }

        @Override
        public String getPath() {
            return p0;
        }

        @Override
        public Date getLastModified() {
            return null;
        }

        @Override
        public String getLastModifiedString() {
            return null;
        }

        @Override
        public ETag getETag() {
            return null;
        }

        @Override
        public String getName() {
            return finalP;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public List<Resource> list() {
            return null;
        }

        @Override
        public String getContentType(MimeMappings mimeMappings) {
            return "text/html"; // fixme
        }

        @Override
        public void serve(Sender sender, HttpServerExchange exchange, IoCallback completionCallback) {
            exchange.startBlocking(); // rarely called (once per login) also served from in mem in production mode
            try {
                exchange.getOutputStream().write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
//            completionCallback.onComplete(exchange, sender);
        }

        @Override
        public Long getContentLength() {
            return Long.valueOf(bytes.length);
        }

        @Override
        public String getCacheKey() {
            return null;
        }

        @Override
        public File getFile() {
            return null;
        }

        @Override
        public File getResourceManagerRoot() {
            return null;
        }

        @Override
        public URL getUrl() {
            return null;
        }
    }

    public static void main(String a[]) {
        Knode knode = new Knode();
        knode.mainStub(a);
        String rp[] = {
            "4k",
            "tmp",
            "../../weblib",
            "../../weblib/nustaq",
            "../../weblib/knockout"
        };
        DynamicResourceManager man = new DynamicResourceManager(true,"app",rp);
        knode.getPathHandler().addPrefixPath("/myapp/", new ResourceHandler(man));
    }

}