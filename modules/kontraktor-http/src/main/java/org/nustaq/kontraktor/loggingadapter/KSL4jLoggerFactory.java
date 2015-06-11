package org.nustaq.kontraktor.loggingadapter;

import org.slf4j.Logger;

/**
 * Created by ruedi on 11/06/15.
 */
public class KSL4jLoggerFactory implements org.slf4j.ILoggerFactory {
    @Override
    public Logger getLogger(String s) {
        return new KWTFLoggerAdapter(s);
    }
}
