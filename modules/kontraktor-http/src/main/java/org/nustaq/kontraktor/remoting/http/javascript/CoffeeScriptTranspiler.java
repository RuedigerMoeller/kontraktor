package org.nustaq.kontraktor.remoting.http.javascript;

import java.io.File;
import java.util.List;

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
