package org.nustaq.kontraktor.util;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.annotations.CallerSideMethod;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Created by ruedi on 24.08.14.
 *
 * A wrapper for logging + metrics.
 *
 */
public class Log extends Actor<Log> {

    public static final int INFO = 1;
    public static final int WARN = 2;
    public static final int ERROR = 3;

    public static Log Lg = Actors.AsActor(Log.class,100000);
    public static void Info( Object source, String msg ) {
        Lg.info(source,msg);
    }

    public static interface LogWrapper {
        public void msg(Thread t, int severity, Object source, Throwable ex, String msg);
    }

    LogWrapper logger = new LogWrapper() {
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
        @Override
        public void msg(Thread t, int sev, Object source, Throwable ex, String msg) {
            if ( severity <= sev ) {
                if ( source == null )
                    source = "NULL";
                else
                    source = source.getClass().getSimpleName();
                String tname = t == null ? "-" : t.getName();
                String svString = "";
                switch (sev) {
                    case WARN: svString = "! "; break;
                    case ERROR: svString = "!! "; break;
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

    int severity = 0;

    public void $init( LogWrapper delegate ) {
        this.logger = delegate;
    }

    public void $setSeverity(int severity) {
        this.severity = severity;
    }

    /////////////////////////////////////////////////////////////////////
    // caller side wrappers are here to enable stacktrace capture etc.
    //

    @CallerSideMethod public void infoLong(Object source, Throwable ex, String msg) {
        $msg(Thread.currentThread(), INFO, source, ex, msg);
    }

    @CallerSideMethod public void info( Object source, String msg ) {
        $msg(Thread.currentThread(), INFO, source, null, msg);
    }

    @CallerSideMethod public void warnLong( Object source, Throwable ex, String msg ) {
        $msg(Thread.currentThread(), WARN, source, ex, msg);
    }

    @CallerSideMethod public void warn( Object source, String msg ) {
        $msg(Thread.currentThread(), WARN, source, null, msg);
    }

    @CallerSideMethod public void error( Object source, Throwable ex, String msg ) {
        $msg(Thread.currentThread(), ERROR, source, ex, msg);
    }

    ////////////////////////////

    // async mother method
    public void $msg( Thread t, int severity, Object source, Throwable ex, String msg ) {
        logger.msg( t, severity,source,ex,msg);
    }


}
