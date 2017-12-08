package org.nustaq.kontraktor.webapp.javascript;

import org.nustaq.kontraktor.util.Log;

import java.io.UnsupportedEncodingException;

/**
 * a processor applied to plain javascript snippets.
 * FIXME: as this was introduced somewhat late, common post processors such as JSMin etc. do not implement
 * FIXME: this interface yet. Needs some refactoring throughout the bundling pipeline.
 */
public interface JSPostProcessor {

    default byte[] postProcess(byte[] currentJS, JSPostProcessorContext context) {
        try {
            return postProcess(new String(currentJS,"UTF-8"),context).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.Error(this,e);
            return (""+e).getBytes();
        }
    }

    default String postProcess(String currentJS, JSPostProcessorContext context) {
        try {
            return new String(postProcess(currentJS.getBytes("UTF-8"),context),"UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.Error(this,e);
            return ""+e;
        }
    }

    public static class JSPostProcessorContext {
    }

}
