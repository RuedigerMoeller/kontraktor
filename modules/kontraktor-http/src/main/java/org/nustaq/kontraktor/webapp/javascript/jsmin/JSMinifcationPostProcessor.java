package org.nustaq.kontraktor.webapp.javascript.jsmin;

import org.nustaq.kontraktor.webapp.javascript.JSPostProcessor;

public class JSMinifcationPostProcessor implements JSPostProcessor {

    @Override
    public byte[] postProcess(byte[] currentJS, JSPostProcessorContext context) {
        return JSMin.minify(currentJS);
    }
}
