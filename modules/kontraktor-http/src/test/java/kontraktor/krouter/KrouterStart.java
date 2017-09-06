package kontraktor.krouter;

import org.junit.Test;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.WebSocketPublisher;
import org.nustaq.kontraktor.routers.HotColdFailoverKrouter;
import org.nustaq.kontraktor.routers.HotHotFailoverKrouter;
import org.nustaq.kontraktor.routers.Routing;

public class KrouterStart {

    @Test
    public void startWS() throws InterruptedException {
        Routing.start(
            HotColdFailoverKrouter.class,
//            new TCPNIOPublisher()
//                .serType(SerializerType.FSTSer)
//                .port(6667),
            new WebSocketPublisher()
                .hostName("localhost")
                .urlPath("/binary")
                .port(8888)
                .serType(SerializerType.FSTSer)
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
        while (true) {
            Thread.sleep(1000000l);
        }
    }

}
