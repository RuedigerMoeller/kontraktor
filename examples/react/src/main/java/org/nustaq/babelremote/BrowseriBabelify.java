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
        WebSocketConnectable webSocketConnectable =
            new WebSocketConnectable(BrowseriBabelify.class, "ws://localhost:3999/ws")
                .coding(new Coding(SerializerType.JsonNoRef, BrowseriBabelify.BabelResult.class ));
        webSocketConnectable.connect( (xy,e) -> System.out.println("disconnected "+xy) ).then(
            (actor,err) -> {
                if ( actor != null ) {
                    BrowseriBabelify b = (BrowseriBabelify) actor;
                    try {
                        b.browserify("./src/main/web/client/index.jsx")
                            .then( (r,e) -> {
                                System.out.println(r);
                            });

//                        String pathname = "/home/ruedi/projects/kontraktor/examples/react/src/main/web/client/index.jsx";
//                        byte[] bytes = Files.readAllBytes(new File(pathname).toPath());
////                        String options = "{ 'presets': ['react','es2015'] }".replace('\'', '\"');
//                        String options = "{ 'presets': ['react'] }".replace('\'', '\"');
//                        b.transform(new String(bytes,"UTF-8"), options)
//                            .then( (r,e) -> {
//                                System.out.println(r);
//                            });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.Info(BrowseriBabelify.class,"could not connect service");
                }
            }
        );
    }
}
