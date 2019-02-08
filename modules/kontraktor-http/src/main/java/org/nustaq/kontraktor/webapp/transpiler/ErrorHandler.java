package org.nustaq.kontraktor.webapp.transpiler;

import org.nustaq.kontraktor.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * fumbled in, so global. Transpiler need to reset individually
 */
public class ErrorHandler {

    public static ErrorHandler singleton = new ErrorHandler(); // replace by a subclass if required
    public static ErrorHandler get() {
        return singleton;
    }

    List<String> errors = new ArrayList<>();

    public void reset() {
        errors.clear();
    }

    public List<String> getErrors() {
        return errors;
    }

    public void add(Class sender, String error, File f ) {
        String absolutePath = f.getAbsolutePath();
        try {
            absolutePath = f.getCanonicalPath();
        } catch (IOException e) {
            Log.Error(this,e);
        }
        error = "@"+ absolutePath +": "+error;
        Log.Warn(sender,error);
        errors.add(error);
        if ( errors.size() > 100 ) {
            List<String> oldErr = this.errors;
            this.errors = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                this.errors.add(oldErr.get(oldErr.size()-20+i));
            }
        }
    }

}
