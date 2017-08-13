package org.nustaq.kontraktor.remoting.http.javascript;

import java.io.File;
import java.util.Set;

/**
 * Created by ruedi on 21.05.16.
 *
 * hook to transpile files dynamically. Hooks can be registered in the http4k builder class 'BldResPath'
 */
public interface TranspilerHook {

    /**
     * indicates a file is being accessed. in dev mode this is called always (so its up to the implementation
     * to check for filedate/modification).
     *
     * In order to run an external transpiler, just run transpilation synchronous such that the given file is
     * updated and return null.
     *
     * Alternatively an implementation might choose to just transpile dynamically and directly return byte[].
     *
     * NOTE: transpilation is static. this means in production mode it will be called once on first resource access.
     *
     * @param f - the file being accessed (after resourcepath resolvment)
     * @return either (transpiled) file content or null.
     * Should throw an exception in case an error occured during transpile.
     */
    byte[] transpile(File f) throws TranspileException;

    default byte[] transpile(File f, FileResolver resolver, Set<String> alreadyResolved) {
        return transpile(f);
    }
}
