package org.nustaq.kontraktor.remoting.http.javascript;

import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.FileResource;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.util.ETag;
import io.undertow.util.MimeMappings;
import org.nustaq.kontraktor.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * adapts kontraktors js + html snippets dependency management to undertow
 *
 */
public class DynamicResourceManager extends FileResourceManager {

    boolean devMode = false;
    DependencyResolver dependencyResolver;
    String prefix = "";
    String lookupPrefix;
    String mergedPrefix;
    /**
     * in case not run in devmode, store lookup results in memory,
     * so file crawling is done once after server restart.
     */
    ConcurrentHashMap<String,Resource> lookupCache = new ConcurrentHashMap<>();

    public DynamicResourceManager(boolean devMode, String prefix, String rootComponent, String ... resourcePath) {
        super(new File("."), 100);
        this.devMode = devMode;
        setPrefix(prefix);
        dependencyResolver = new DependencyResolver(prefix+"/"+lookupPrefix,prefix+"/"+mergedPrefix,rootComponent,resourcePath);
        setBase(dependencyResolver.locateComponent(rootComponent).get(0));
        if ( devMode )
            Log.Warn(this, "Dependency resolving is running in *DEVELOPMENT MODE*. Turn off development mode to cache aggregated resources");
        else
            Log.Info(this, "Dependency resolving is running in *PRODUCTION MODE*. Turn on development mode for script-refresh-on-reload and per file javascript debugging");
    }

    public void setPrefix(String prefix) {
        while (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        this.prefix = prefix;
        lookupPrefix = "lookup/";
        mergedPrefix = "merged/";
    }

    public boolean isDevMode() {
        return devMode;
    }

    @Override
    public Resource getResource(String p0) {
        String p = p0;
        if (p.startsWith("/")) {
            p = p.substring(1);
        }
        String orgP = p;
        if ( ! isDevMode() ) {
            if (lookupCache.get(orgP) != null) {
                return lookupCache.get(orgP);
            }
        }
        if ( p.startsWith(lookupPrefix) ) {
            p = p.substring(lookupPrefix.length());
            if ( p.startsWith("+") ) {
                String[] split = p.substring(1).split("\\+");
                List<String> filesInDirs = dependencyResolver.findFilesInDirs( (comp,finam) ->  {
                    for (int i = 0; i < split.length; i++) {
                        String s = split[i];
                        if ( finam.equals(s) ) {
                            return true;
                        }
                    }
                    return false;
                });
                byte bytes[] = dependencyResolver.mergeBinary(filesInDirs); // trouble with textmerging
                return mightCache(orgP,new MyResource(p0, p, bytes,"text/html")); //fixme mime
            } else {
                File file = dependencyResolver.locateResource(p);
                if ( file != null ) {
                    return mightCache(orgP, new FileResource(file, this, p0));
                } else {
                    return null;
                }
            }
        } else if ( p.startsWith(mergedPrefix) ) { // expect simple ending like '.js'
            p = p.substring(mergedPrefix.length());
            final String finalP = p;
            List<String> filesInDirs = dependencyResolver.findFilesInDirs( (comp,finam) -> finam.endsWith(finalP));
            byte[] bytes;
            if ( finalP.endsWith(".css") ) {
                bytes = dependencyResolver.mergeBinary(filesInDirs); // trouble with textmerging
            } else {
                bytes = dependencyResolver.mergeTextSnippets(filesInDirs,"","");
            }
            return mightCache(orgP, new MyResource(p0, finalP, bytes,"text/html"));//fixme mime
        } else if ( p.equals("js") || p.startsWith("+") ) { // expect simple ending like '.js' or +name+name+name
            List<String> filesInDirs;
            final String finalP = p;
            if ( p.startsWith("+") ) {
                String[] split = p.substring(1).split("\\+");
                filesInDirs = dependencyResolver.findFilesInDirs( (comp,finam) ->  {
                    for (int i = 0; i < split.length; i++) {
                        String s = split[i];
                        if ( finam.equals(s) || (comp.equals(s)&&finam.endsWith(".js")) ) {
                            return true;
                        }
                    }
                    return false;
                });
            } else {
                filesInDirs = dependencyResolver.findFilesInDirs( (comp,finam) -> finam.endsWith(".js"));
            }
            byte[] bytes = null;
            if ( devMode )
                bytes = dependencyResolver.createScriptTags(filesInDirs);
            else
                bytes = dependencyResolver.mergeScripts(filesInDirs);
            return mightCache( orgP, new MyResource(p0, finalP, bytes, "text/javascript") );
        }
        return super.getResource(p0);
    }

    private Resource mightCache(String key, Resource fileResource) {
        if ( ! isDevMode() ) {
            lookupCache.put(key,fileResource);
        }
        return fileResource;
    }

    protected static class MyResource implements Resource {
        protected String p0;
        protected String finalP;
        protected byte[] bytes;
        protected String resType;

        public MyResource(String p0, String finalP, byte[] bytes, String resType ) {
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
            return resType;
        }

        @Override
        public void serve(Sender sender, HttpServerExchange exchange, IoCallback completionCallback) {
            exchange.startBlocking(); // rarely called (once per login) also served from mem in production mode
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

}