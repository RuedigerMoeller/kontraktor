package org.nustaq.kontraktor.webapp.transpiler;

import java.io.File;

/**
 * Created by ruedi on 22.05.16.
 */
public class CoffeeScriptTranspiler extends CLICommandTranspiler {

    public CoffeeScriptTranspiler() {
        super(".coffee");
    }

    @Override
    protected String[] createCMDLine(File targetJSFile, File source) {
        return new String[] { "coffee","-c",source.getAbsolutePath()};
    }
}
