package org.nustaq.kontraktor.webapp.transpiler;

import org.nustaq.kontraktor.webapp.babel.BabelOpts;
import org.nustaq.kontraktor.webapp.babel.BabelResult;
import org.nustaq.kontraktor.webapp.babel.BrowseriBabelify;

import java.io.*;

/**
 * Created by ruedi on 30.06.17.
 */
public class JSXWithBabelTranspiler implements TranspilerHook {

    BabelOpts opts = new BabelOpts();

    public JSXWithBabelTranspiler opts(final BabelOpts opts) {
        this.opts = opts;
        return this;
    }

    public BabelOpts getOpts() {
        return opts;
    }

    @Override
    public byte[] transpile(File f) throws TranspileException {
        try {
            BabelResult result = BrowseriBabelify.get().browserify(f.getAbsolutePath(),opts).await();
            if (result.code!=null)
                return result.code.toString().getBytes("UTF-8");
            else {
                System.out.println(result.err);
                return result.err.toString().getBytes("UTF-8");
            }
        } catch (Exception e) {
            throw new TranspileException(e);
        }
    }

}
