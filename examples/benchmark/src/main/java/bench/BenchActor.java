package bench;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.remoting.base.RemoteRefPolling;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.HttpPublisher;
import org.nustaq.kontraktor.remoting.http.undertow.WebSocketPublisher;
import org.nustaq.kontraktor.remoting.tcp.TCPPublisher;

public class BenchActor<T> extends Actor<BenchActor> {

    int lastVal = 0;
    public IPromise<Integer> echo(int value) {
        return new Promise(value);
    }
    public void noEcho(int value) {
        lastVal = value;
    }

    public IPromise<Integer> dummy() {
        System.out.println("wait "+lastVal);
        return new Promise(lastVal);
    }

    public static void main(String[] args) {
        BackOffStrategy.SLEEP_NANOS = 1*1000*1000;
        RemoteRefPolling.EMPTY_Q_BACKOFF_WAIT_MILLIS = 1;

        BenchActor benchActor = Actors.AsActor(BenchActor.class);

        new TCPPublisher()
            .facade(benchActor)
            .port(8181)
            .serType(SerializerType.FSTSer)
            .publish();

        new WebSocketPublisher()
            .facade(benchActor)
            .hostName("localhost")
            .port(8182)
            .urlPath("ws")
            .serType(SerializerType.FSTSer)
            .publish();

        new HttpPublisher(benchActor,"localhost","htlp",8182)
            .serType(SerializerType.FSTSer)
            .publish();
    }
}