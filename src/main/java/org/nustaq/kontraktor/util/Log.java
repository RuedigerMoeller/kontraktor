/*
Kontraktor Copyright (c) Ruediger Moeller, All rights reserved.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3.0 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

See https://www.gnu.org/licenses/lgpl.txt
*/

package org.nustaq.kontraktor.util;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.annotations.CallerSideMethod;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by ruedi on 24.08.14.
 *
 * A wrapper for logging + metrics. This logger facade is asynchronous (so does not block by IO).
 * In order to redirect logging, use Log.Lg.setLogWrapper( .. );
 *
 */
public class Log extends Actor<Log> {

    public static final int DEBUG = 0;
    public static final int INFO = 1;
    public static final int WARN = 2;
    public static final int ERROR = 3;

    public static Log Lg = Actors.AsActor(Log.class,100000);

    /**
     * @param level = Log.DEBUG | Log.INFO | Log.WARN | Log.ERROR
     */
    public static void setLevel( int level ) {
        Lg.setSeverity(level);
    }
    public static void Info( Object source, String msg ) {
        Lg.info(source, msg);
    }
    public static void Info( Object source, Throwable ex ) {
        Lg.infoLong(source, ex, null);
    }
    public static void Debug( String msg ) {
        Lg.debug(null, msg);
    }
    public static void Debug( Object source, String msg ) {
        Lg.debug(source,msg);
    }
    public static void Debug( Object source, Throwable th ) {
        Lg.debug(source,th);
    }
    public static void Info( Object source, Throwable t, String msg ) {
        Lg.infoLong(source,t,msg);
    }
    public static void Warn( Object source, Throwable t, String msg ) {
        Lg.warnLong(source,t,msg);
    }
    public static void Warn( Object source, String msg ) {
        Lg.warnLong(source,null,msg);
    }
    public static void Warn( Object source, Throwable ex ) {
        Lg.warnLong(source,ex,null);
    }
    public static void Error(Object source, String s) {
        Error(source,null,s);
    }
    public static void Error(Object source, Throwable th) {
        Error(source,th,null);
    }
    public static void Error(Object source, Throwable th, String s) {
        Lg.error(source,th,s);
    }

    public static interface LogWrapper {
        public void msg(Thread t, int severity, Object source, Throwable ex, String msg);
    }

    public LogWrapper defaultLogger = new LogWrapper() {
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
        boolean initialized = false;
        @Override
        public void msg(Thread t, int sev, Object source, Throwable ex, String msg) {
            if ( ! initialized ) {
                initialized = true;
                Thread.currentThread().setName("kontraktor async logger");
            }
            if ( severity <= sev ) {
                if ( source == null )
                    source = "null";
                else if ( source instanceof String == false ) {
                    if ( source instanceof Class ) {
                        source = ((Class) source).getName();
                    } else
                        source = source.getClass().getSimpleName();
                }
                String tname = t == null ? "-" : t.getName();
                String svString = "I ";
                switch (sev) {
                    case WARN: svString = "W "; break;
                    case DEBUG: svString = "D "; break;
                    case ERROR: svString = "E "; break;
                }
                System.out.println(svString+formatter.format(new Date())+" : "+ tname +" : "+source+" : "+msg);
                if ( ex != null ) {
                    if ( sev == INFO ) {
                        System.out.println(ex.toString());
                    } else {
                        ex.printStackTrace(System.out);
                    }
                }
            }
        }
    };

    LogWrapper logger = defaultLogger;

    volatile int severity = INFO;

    public void setLogWrapper(LogWrapper delegate) {
        this.logger = delegate;
    }


    public void setSeverity(int severity) {
        this.severity = severity;
    }

    /////////////////////////////////////////////////////////////////////
    // caller side wrappers are here to enable stacktrace capture etc.
    //

    @CallerSideMethod public int getSeverity() {
        return getActor().severity;
    }

    @CallerSideMethod public void resetToSysout() {
        this.logger = defaultLogger;
    }

    @CallerSideMethod public void infoLong(Object source, Throwable ex, String msg) {
        self().msg(Thread.currentThread(), INFO, source, ex, msg);
    }

    @CallerSideMethod public void debug( Object source, String msg ) {
        self().msg(Thread.currentThread(), DEBUG, source, null, msg);
    }

    @CallerSideMethod public void debug( Object source, Throwable msg ) {
        self().msg(Thread.currentThread(), DEBUG, source, msg, null);
    }

    @CallerSideMethod public void info( Object source, String msg ) {
        self().msg(Thread.currentThread(), INFO, source, null, msg);
    }

    @CallerSideMethod public void warnLong( Object source, Throwable ex, String msg ) {
        self().msg(Thread.currentThread(), WARN, source, ex, msg);
    }

    @CallerSideMethod public void warn( Object source, String msg ) {
        self().msg(Thread.currentThread(), WARN, source, null, msg);
    }

    @CallerSideMethod public void error( Object source, Throwable ex, String msg ) {
        self().msg(Thread.currentThread(), ERROR, source, ex, msg);
    }

    ////////////////////////////

    // async mother method
    public void msg( Thread t, int severity, Object source, Throwable ex, String msg ) {
        logger.msg( t, severity,source,ex,msg);
    }


}
