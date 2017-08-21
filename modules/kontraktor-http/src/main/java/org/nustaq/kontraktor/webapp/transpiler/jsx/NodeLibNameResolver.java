package org.nustaq.kontraktor.webapp.transpiler.jsx;

import org.nustaq.kontraktor.webapp.javascript.FileResolver;

import java.io.File;

public interface NodeLibNameResolver extends FileResolver {

    public String getFinalLibName(File requiredIn, FileResolver res, String requireText);
}
