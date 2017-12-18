package org.nustaq.kontraktor.webapp.transpiler.jsx;

import org.nustaq.kontraktor.webapp.javascript.FileResolver;
import org.nustaq.kontraktor.webapp.transpiler.JSXIntrinsicTranspiler;

import java.io.File;

public class WatchedFile {
    File file;
    long lastModified;
    JSXIntrinsicTranspiler transpiler;
    FileResolver resolver;

    public WatchedFile(File file, JSXIntrinsicTranspiler transpiler, FileResolver resolver) {
        this.file = file;
        this.transpiler = transpiler;
        this.resolver = resolver;
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
}
