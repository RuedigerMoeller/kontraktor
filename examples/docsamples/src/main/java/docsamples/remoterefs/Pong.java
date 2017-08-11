package docsamples.remoterefs;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.util.Log;

public class Pong<T extends Pong> extends Actor<T> {

    public void receivePing(Ping ping) {
        Log.Info(this,"receivePing");
        delayed(1000, () -> ping.receivePong(self()));
    }

    public static void main(String[] args) {
        Pong pong = AsActor(Pong.class);
        new TCPNIOPublisher()
            .port(9080)
            .serType(SerializerType.FSTSer)
            .facade(pong)
            .publish( x -> Log.Info(Pong.class,"disconnected "+x));

        // we need to initiate ping pong. Assumes Ping is already running
        Ping ping = (Ping) new TCPConnectable()
            .host("localhost")
            .port(9080)
            .actorClass(Ping.class)
            .serType(SerializerType.FSTSer)
            .connect()
            .await();

        ping.receivePong(pong);
    }
}
