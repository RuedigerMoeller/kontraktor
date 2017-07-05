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

    /**
     * using nashorn to run babel (you should use a recent jdk).
     * initialization is slow, however in production mode files are transformed once on first request.
     *
     * if this becomes a point of failure (e.g. nashorn bugs), this can be replaced by running
     * nodejs/babel in a child process.
     *
     * @param f - the file being accessed (after resourcepath resolvment)
     * @return
     * @throws TranspileException
     */
//    @Override
    public byte[] transpile_nashorn(File f) throws TranspileException {
        FileReader babelScript = null;
        try {
            if ( engine == null ) {
                Log.Info(this,"initializing nashorn + babel, THIS TAKES SEVERAL SECONDS ONCE");
                babelScript = new FileReader("src/main/web/bower_components/babel-standalone/babel.js");
                engine = new ScriptEngineManager().getEngineByMimeType("text/javascript");
                bindings = new SimpleBindings();
                engine.eval(babelScript, bindings);
            }

            String fileContentent = resolveImports(f, new HashSet<>()).collect(Collectors.joining("\n"));

            bindings.put("input", fileContentent);
            Object output = engine.eval("Babel.transform(input, { presets: ["+presets+"] }).code", bindings);
            if (LogResult)
                Log.Info(this,output.toString());
            return output.toString().getBytes("UTF-8");
        } catch (Exception e) {
            throw new TranspileException(e);
        }
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

    protected Stream<String> resolveImports(File f, Set<String> processed) throws IOException {
        String code = new String(Files.readAllBytes(f.toPath()), "UTF-8");
        String canonicalPath = f.getCanonicalPath();
        if (processed.contains(canonicalPath))
            return Stream.of("");
        processed.add(canonicalPath);
        return new BufferedReader(new StringReader(code)).lines()
            .flatMap( line -> {
                String li = line.trim();
                if ( li.startsWith("'#include ") || li.startsWith("\"#include ") ) {
                    li = li.substring(1,li.length()-2);
                    String[] split = li.split(" ");
                    if ( split.length != 2 ) {
                        throw new TranspileException("invalid #include:"+line);
                    }
                    File toIncl = new File(f.getParentFile(),split[1]);
                    if ( ! toIncl.exists() ) {
                        throw new TranspileException("unable to find: "+toIncl);
                    }
                    try {
                        return resolveImports(toIncl,processed);
                    } catch (IOException e) {
                        throw new TranspileException(e);
                    }
                }
                return Stream.of(line);
            });
//            .collect(Collectors.joining("\n"));
    }
}
