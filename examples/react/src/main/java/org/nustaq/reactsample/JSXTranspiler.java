package org.nustaq.reactsample;

import org.nustaq.babelremote.BrowseriBabelify;
import org.nustaq.kontraktor.remoting.http.javascript.TranspileException;
import org.nustaq.kontraktor.remoting.http.javascript.TranspilerHook;
import org.nustaq.kontraktor.util.Log;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import java.io.*;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by ruedi on 30.06.17.
 */
public class JSXTranspiler implements TranspilerHook {
    public static boolean LogResult = true;

    ScriptEngine engine;
    SimpleBindings bindings;
    String presets;

    public JSXTranspiler() {
        this("'react','es2015'");
    }

    public JSXTranspiler(String presets) {
        this.presets = presets;
    }

    @Override
    public byte[] transpile(File f) throws TranspileException {
        try {
            BrowseriBabelify.BabelResult result = BrowseriBabelify.get().browserify(f.getAbsolutePath()).await();
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
