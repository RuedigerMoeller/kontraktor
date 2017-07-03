package org.nustaq.babelremote;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;

/**
 * Created by ruedi on 03.07.17.
 */
public class Babel extends Actor<Babel> {

    public IPromise<String> hello( String script ) {
        return resolve("Hello "+script);
    }

    public static void main(String[] args) {
        WebSocketConnectable webSocketConnectable =
            new WebSocketConnectable(Babel.class, "ws://localhost:3999/ws")
                .coding(new Coding(SerializerType.JsonNoRef));
        webSocketConnectable.connect( (xy,e) -> System.out.println("discon "+xy) ).then(
            (actor,err) -> {
                System.out.println(actor+" "+err);
                if ( actor != null ) {
                    Babel b = (Babel) actor;
                    b.hello("pok").then( (r,e) -> {
                        System.out.println("res: "+r+" "+e);
                    });
                }
            }
        );
    }
}
