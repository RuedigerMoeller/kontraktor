package docsamples.callbacks;

import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;

public class CBSampleClient {

    public static void main(String[] args) {
        CBSampleServer server = (CBSampleServer) new TCPConnectable()
            .actorClass(CBSampleServer.class)
            .serType(SerializerType.FSTSer).host("localhost").port(9090)
            .connect((connector,actor) -> System.out.println("disconnected from server"))
            .await();

        server.withCallback("beep", (res,err) -> System.out.println("res:"+res+" err:"+err));
        server.timePing(10,(time,err) -> System.out.println("time:"+time+" err:"+err));
    }
}
