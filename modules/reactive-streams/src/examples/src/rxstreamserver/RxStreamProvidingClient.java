package rxstreamserver;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.reactivestreams.ReaktiveStreams;
import org.nustaq.kontraktor.reactivestreams.RxPublisher;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;

import java.util.Date;
import java.util.stream.LongStream;

/**
 * Created by ruedi on 05/07/15.
 */
public class RxStreamProvidingClient {

    public static void main(String[] args) {
        // FIXME: cancel test. No error on connection close delivered.

        RxStreamServer remoteRef = (RxStreamServer) new TCPConnectable(RxStreamServer.class, "localhost", 7890).connect().await();

        remoteRef.putStream("SomeNumbers", ReaktiveStreams.get().produce(LongStream.range(13,139)), false );

        // await based variant of subscribing timer (compare with other client):
        RxPublisher timer = remoteRef.listen("TIME").await();
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
