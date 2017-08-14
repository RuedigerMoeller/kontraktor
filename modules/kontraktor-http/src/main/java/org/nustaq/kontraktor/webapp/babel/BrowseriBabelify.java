package org.nustaq.kontraktor.webapp.babel;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;

import java.io.File;

/**
 * Created by ruedi on 03.07.17.
 */
public class BrowseriBabelify extends Actor<BrowseriBabelify> {

    public static String url = "ws://localhost:3999/browseribabelify";

    static protected BrowseriBabelify singleton;
    public static BrowseriBabelify get() {
        synchronized (BrowseriBabelify.class) {
            if (singleton==null) {
                WebSocketConnectable webSocketConnectable =
                    new WebSocketConnectable(BrowseriBabelify.class, url)
                        .coding(new Coding(SerializerType.JsonNoRef, BabelResult.class, BabelOpts.class ));
                singleton = (BrowseriBabelify) webSocketConnectable.connect( (xy, e) -> System.out.println("disconnected "+xy) ).await(1000);
            }
        }
        return singleton;

    }

    @CallerSideMethod @Local
    public IPromise<BabelResult> browserify( String filePath, BabelOpts opts ) {
        return browserifyInternal(new File(filePath).getAbsolutePath(), opts);
    }

    // dummy prototype, implementation is on js server side
    public IPromise<BabelResult> browserifyInternal(String filePath, BabelOpts opts) {
        return null;
    }

}
