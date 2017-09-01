package kontraktor.krouter;

import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.HttpPublisher;
import org.nustaq.kontraktor.remoting.http.undertow.WebSocketPublisher;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.routers.*;

public class KrouterMain {

    public static void main(String[] args) {
        AbstractKrouter.start(
            HotColdFailoverKrouter.class,
            new TCPNIOPublisher()
                .serType(SerializerType.FSTSer)
                .port(6667)
//            new WebSocketPublisher()
//                .hostName("localhost")
//                .urlPath("/binary")
//                .port(8888)
//                .serType(SerializerType.FSTSer)
//            new WebSocketPublisher()
//                .hostName("localhost")
//                .urlPath("/json")
//                .port(8888)
//                .serType(SerializerType.JsonNoRef),
//            new HttpPublisher()
//                .hostName("localhost")
//                .urlPath("/http")
//                .port(8888)
//                .serType(SerializerType.JsonNoRef)
        );
    }
}
