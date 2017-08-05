package docsamples.remoterefs;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.util.Log;

public class Ping<T extends Ping> extends Actor<T> {

    public void receivePong(Pong pong ) {
        Log.Info(this,"receivePong");
        delayed(1000, () -> pong.receivePing(self()) );
    }

    public static void main(String[] args) {
        Ping ping = AsActor(Ping.class);
        new TCPNIOPublisher()
            .port(9080)
            .serType(SerializerType.FSTSer)
            .facade(ping)
            .publish( x -> Log.Info(Ping.class,"disconnected "+x));
    }

}
