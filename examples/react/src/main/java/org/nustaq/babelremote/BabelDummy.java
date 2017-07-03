package org.nustaq.babelremote;

import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.WebSocketPublisher;

/**
 * Created by ruedi on 03.07.17.
 *
 * dummy to figure out what should be sent
 */
public class BabelDummy {
    public static void main(String[] args) {
        Babel b = Babel.AsActor(Babel.class);
        new WebSocketPublisher(b,"localhost","ws", 3999)
            .coding(new Coding(SerializerType.JsonNoRef))
            .publish( act -> System.out.println("discon "+act) );
    }
}
