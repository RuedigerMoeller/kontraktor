package org.nustaq.babelremote;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;
import org.nustaq.kontraktor.util.Log;

import java.io.File;
import java.io.Serializable;

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
                        .coding(new Coding(SerializerType.JsonNoRef, BrowseriBabelify.BabelResult.class ));
                singleton = (BrowseriBabelify) webSocketConnectable.connect( (xy, e) -> System.out.println("disconnected "+xy) ).await();
            }
        }
        return singleton;

    }
    // dummystub
    public IPromise<BabelResult> transform( String input, String optionsJson ) {
        return null;
    }

    @CallerSideMethod @Local
    public IPromise<BabelResult> browserify( String filePath ) {
        return browserifyInternal(new File(filePath).getAbsolutePath());
    }

    public IPromise<BabelResult> browserifyInternal( String filePath ) {
        return null;
    }

    public static class BabelResult implements Serializable {
        public String code;
        public String err;

        @Override
        public String toString() {
            return "BabelResult{" +
                "code='" + code + '\'' +
                ", err='" + err + '\'' +
                '}';
        }
    }

    public static void main(String[] args) {
        BrowseriBabelify b = BrowseriBabelify.get();
        try {
            b.browserify("./src/main/web/client/index.jsx").then( (r,e) -> {
                System.out.println(r);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
