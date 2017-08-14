package org.nustaq.kontraktor.webapp.javascript;

import io.undertow.server.handlers.resource.*;
import io.undertow.util.*;

import java.io.*;
import java.util.*;

/**
 */
public class CachedFileResourceManager extends FileResourceManager{
    Date lastStartup; // last startUp, will be returned as LastModifiedDate for cached resources..

    public CachedFileResourceManager(boolean enableCaching  , final File base, long transferMinSize) {
        super(base , transferMinSize );
        this.lastStartup = enableCaching ? new Date() : null ; // note: plain wrong and obviously untested
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
            if ( lastModified == null ) {
                return new Date(); // quick fix for severe cache fail
            }
            return lastModified;
        }

        @Override
        public String getLastModifiedString() {
            return DateUtils.toDateString(getLastModified());
        }
    }
}
