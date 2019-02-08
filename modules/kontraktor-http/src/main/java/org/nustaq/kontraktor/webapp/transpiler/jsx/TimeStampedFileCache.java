package org.nustaq.kontraktor.webapp.transpiler.jsx;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class TimeStampedFileCache<T> {

    static Map<File,String> canonicalCache = new HashMap<>();
    public static String getCanonicalPath(File f) throws IOException {
        String s = canonicalCache.get(f);
        if (s != null )
            return s;
        String canonicalPath = null;
        if ( Files.isSymbolicLink(f.toPath()) )
            canonicalPath = f.getAbsolutePath();
        else
            canonicalPath = f.getCanonicalPath();
        canonicalCache.put(f,canonicalPath);
        return canonicalPath;
    }

    Map<String,T> cache = new HashMap<>();
    public void put(File f, T result) {
        String key = getKey(f);
        cache.put(key,result);
    }

    public T get(File f) {
        return cache.get(getKey(f));
    }

    public String getKey(File f) {
        String path = null;
        try {
            path = getCanonicalPath(f);
        } catch (IOException e) {
            path = f.getAbsolutePath();
        }
        return f.lastModified() + " " + path;
    }

}
