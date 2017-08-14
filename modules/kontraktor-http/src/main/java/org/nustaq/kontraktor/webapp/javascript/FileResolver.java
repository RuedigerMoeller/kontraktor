package org.nustaq.kontraktor.webapp.javascript;

import java.io.File;
import java.util.Set;

public interface FileResolver {

    /**
     * lookup searchpath
     *
     * @param baseDir
     * @param name
     * @param alreadyProcessed
     * @return
     */
    byte[] resolve(File baseDir, String name, Set<String> alreadyProcessed);

    void install(String path, byte[] resolved);
}
