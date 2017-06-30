package org.nustaq.reactsample;

import org.nustaq.kontraktor.remoting.http.javascript.TranspileException;
import org.nustaq.kontraktor.remoting.http.javascript.TranspilerHook;
import org.nustaq.kontraktor.util.Log;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;

/**
 * Created by ruedi on 30.06.17.
 */
public class JSXTranspiler implements TranspilerHook {
    public static boolean LogResult = true;

    ScriptEngine engine;
    SimpleBindings bindings;

    @Override
    public byte[] transpile(File f) throws TranspileException {
        FileReader babelScript = null;
        try {
            if ( engine == null ) {
                Log.Info(this,"initializing nashorn + babel, this takes several seconds");
                babelScript = new FileReader("src/main/web/bower_components/babel-standalone/babel.js");
                engine = new ScriptEngineManager().getEngineByMimeType("text/javascript");
                bindings = new SimpleBindings();
                engine.eval(babelScript, bindings);
            }

            bindings.put("input", new String(Files.readAllBytes(f.toPath()),"UTF-8"));
            Object output = engine.eval("Babel.transform(input, { presets: ['react'] }).code", bindings);
            if (LogResult)
                Log.Info(this,output.toString());
            return output.toString().getBytes("UTF-8");
        } catch (Exception e) {
            throw new TranspileException(e);
        }
    }
}
