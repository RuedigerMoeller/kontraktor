package org.slf4j.impl;

import org.nustaq.kontraktor.loggingadapter.KSL4jLoggerFactory;
import org.slf4j.ILoggerFactory;

/**
 * Created by ruedi on 11/06/15.
 */
public class StaticLoggerBinder {

    public static KSL4jLoggerFactory fac = new KSL4jLoggerFactory();
    public static StaticLoggerBinder instance = new StaticLoggerBinder();

    public static StaticLoggerBinder getSingleton() {
        return instance;
    }

    public ILoggerFactory getLoggerFactory() {
        return fac;
    }

}
