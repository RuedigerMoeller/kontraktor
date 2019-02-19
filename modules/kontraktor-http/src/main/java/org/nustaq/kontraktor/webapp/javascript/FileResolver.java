package org.nustaq.kontraktor.webapp.javascript;

import io.undertow.server.handlers.resource.Resource;

import java.io.File;
import java.util.Map;

public interface FileResolver {

    /**
     * lookup searchpath
     *
     * @param baseDir
     * @param name
     * @param alreadyProcessed
     * @return
     */
    byte[] resolve(File baseDir, String name, Map<String, Object> alreadyProcessed);
    File resolveFile(File baseDir, String name);
    void install(String path, byte[] resolved);
    String resolveUniquePath(File file);
    Resource getResource(String initialPath); // to support pur imports like "import 'x/y/z.css'"

}
