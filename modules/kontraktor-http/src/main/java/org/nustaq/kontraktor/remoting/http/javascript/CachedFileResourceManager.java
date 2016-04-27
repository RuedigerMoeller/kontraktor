package org.nustaq.kontraktor.remoting.http.javascript;

import io.undertow.server.handlers.resource.*;
import io.undertow.util.*;

import java.io.*;
import java.util.*;

/**
 * Created by juptr on 27.04.16.
 */
public class CachedFileResourceManager extends FileResourceManager{
    Date lastStartup; // last startUp, will be returned as LastModifiedDate for cached resources..

    public CachedFileResourceManager(boolean enableCaching  , final File base, long transferMinSize) {
        super(base , transferMinSize );
        this.lastStartup = enableCaching ? new Date() : null ;
    }


    protected FileResource getFileResource(final File file, final String path) throws IOException {
            return new MyFileResource(file, this, path, lastStartup );
    }

    class MyFileResource extends  FileResource{
        Date lastModified;
        public MyFileResource(final File file, final FileResourceManager manager, String path, Date lastStartUp) {
            super(file, manager, path);
            lastModified = lastStartUp;
        }

        @Override
        public Date getLastModified() {
            return lastModified;
        }

        @Override
        public String getLastModifiedString() {
            return DateUtils.toDateString(lastModified);
        }
    }
}
