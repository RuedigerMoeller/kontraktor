package org.nustaq.kontraktor.webapp.transpiler.jsx;

import org.nustaq.kontraktor.webapp.javascript.FileResolver;
import org.nustaq.kontraktor.webapp.transpiler.JSXIntrinsicTranspiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WatchedFile {
    File file;
    long lastModified;
    JSXIntrinsicTranspiler transpiler;
    FileResolver resolver;
    private String webPath;

    public WatchedFile(File file, JSXIntrinsicTranspiler transpiler, FileResolver resolver, String finalLibName) {
        this.file = file;
        this.transpiler = transpiler;
        this.resolver = resolver;
        this.webPath = finalLibName;
        updateTS();
    }

    public File getFile() {
        return file;
    }

    public long getLastModified() {
        return lastModified;
    }

    public JSXIntrinsicTranspiler getTranspiler() {
        return transpiler;
    }

    public void updateTS() {
        lastModified = file.lastModified();
    }

    @Override
    public String toString() {
        return "WatchedFile{" +
            "file=" + file +
            ", lastModified=" + lastModified +
            ", transpiler=" + transpiler +
            ", resolver=" + resolver +
            '}';
    }

    public String getWebPath() {
        return webPath;
    }

}
