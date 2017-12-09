package org.nustaq.kontraktor.webapp.javascript.clojure;

import com.google.javascript.jscomp.*;
import com.google.javascript.jscomp.Compiler;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.webapp.javascript.JSPostProcessor;
import java.io.IOException;
import java.util.ArrayList;

/**
 * uses blocking IO only applicable for build steps or build triggering initial request
 */
public class ClojureJSPostProcessor implements JSPostProcessor {

    @Override
    public String postProcess(String currentJS, JSPostProcessorContext context) {
        try {
            Compiler compiler = new Compiler();

            CompilerOptions options = new CompilerOptions();
            options.setLanguageOut(CompilerOptions.LanguageMode.ECMASCRIPT5);

            CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);

            // To get the complete set of externs, the logic in
            // CompilerRunner.getDefaultExterns() should be used here.
            SourceFile extern = SourceFile.fromCode("xxxx.js", "");

            // The dummy input name "input.js" is used here so that any warnings or
            // errors will cite line numbers in terms of input.js.
            SourceFile input = SourceFile.fromCode("input.js", currentJS);

            ArrayList in = new ArrayList();
            in.add(input);
            // compile() returns a Result, but it is not needed here.
            Result compile = compiler.compile(CommandLineRunner.getBuiltinExterns(CompilerOptions.Environment.BROWSER), in, options);
//            System.out.println(compile);

            // The compiler is responsible for generating the compiled code; it is not
            // accessible via the Result.
            String s = compiler.toSource();
//            System.out.println(s);
            return s;
        } catch (Exception e) {
            Log.Warn(this, e);
        }
        return currentJS;
    }


}
