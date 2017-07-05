package org.nustaq.reactsample;

import org.nustaq.babelremote.BabelOpts;
import org.nustaq.babelremote.BabelResult;
import org.nustaq.babelremote.BrowseriBabelify;
import org.nustaq.kontraktor.remoting.http.javascript.TranspileException;
import org.nustaq.kontraktor.remoting.http.javascript.TranspilerHook;

import javax.script.ScriptEngine;
import javax.script.SimpleBindings;
import java.io.*;

/**
 * Created by ruedi on 30.06.17.
 */
public class JSXTranspiler implements TranspilerHook {

    BabelOpts opts = new BabelOpts();

    public JSXTranspiler opts(final BabelOpts opts) {
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
