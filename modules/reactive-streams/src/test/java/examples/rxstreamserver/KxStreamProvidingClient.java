package examples.rxstreamserver;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.reactivestreams.KxReactiveStreams;
import org.nustaq.kontraktor.reactivestreams.KxPublisher;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;

import java.util.Date;
import java.util.stream.LongStream;

/**
 * Created by ruedi on 05/07/15.
 */
public class KxStreamProvidingClient {

    public static void main(String[] args) {
        // FIXME: cancel test. No error on connection close delivered.

        KxStreamServer remoteRef = (KxStreamServer) new TCPConnectable(KxStreamServer.class, "localhost", 7890).connect().await();

        remoteRef.putStream("SomeNumbers", KxReactiveStreams.get().produce(LongStream.range(13,139)), false );

        // await based variant of subscribing timer (compare with other client):
        KxPublisher timer = remoteRef.listen("TIME").await();
        // could also subscribe with arbitrary RxStreams subscriber.
        // as RxPublisher extends reactivestreams.Publisher
        timer.subscribe((time, err) -> {
            if (Actors.isError(err)) {
                System.out.println("error:" + err);
            } else if (Actors.isComplete(err)) {
                System.out.println("complete. connection closed ?"); // should not happen as infinite stream
            } else {
                System.out.println("received:" + new Date((Long) time));
            }
        });
    }

}
