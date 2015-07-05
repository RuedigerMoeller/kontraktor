package rxstreamserver;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.reactivestreams.EventSink;
import org.nustaq.kontraktor.reactivestreams.ReaktiveStreams;
import org.nustaq.kontraktor.reactivestreams.RxPublisher;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;

import java.util.stream.IntStream;

/**
 * Created by ruedi on 05/07/15.
 *
 * By default using "server" one stream is published on the network on a single address (e.g. localhost:7777). Once the stream is complete,
 * the associated network connection will terminate. Due to network realted latencies, it takes a while until the address
 * can be reused. Therefore this style of exposing streams is mostly suited for "infinit streams". "onComplete" then
 * basically signals connection close.
 *
 * In order to multiplex several streams using a single network connector RxStreamServer can be used.
 *
 */
public class RxStreamServer extends Actor<RxStreamServer> {

    EventSink<Long> timeSink;
    RxPublisher<Long> timeStream;

    public void init() {
        timeSink = new EventSink<Long>();
        // EventSink is *not* remoteable,
        // need create an remoteable async publisher (is actor, so remoteable)
        timeStream = timeSink.asyncMap( l -> l );
        tick();
    }

    // send local time to stream each second (if not blocked by slow subscribers)
    public void tick() {
        if ( ! isStopped() ) {
            timeSink.offer(System.currentTimeMillis());
            delayed(1000,() -> self().tick());
        }
    }

    /**
     * creates a new dedicated stream for given client.
     *
     * further parameters could add entitlement or query-alike functionality
     *
     * @param streamId
     * @return
     */
    public IPromise<RxPublisher> createStream( String streamId, int start, int end ) {
        if ( "NUMBERS".equals(streamId) ) {
            return new Promise<>(ReaktiveStreams.get().produce(IntStream.range(start,end)));
        }
        if ( "STRINGS".equals(streamId) ) {
            return new Promise<>(ReaktiveStreams.get().produce(IntStream.range(start,end)).map( i -> ""+i ));
        }
        return reject("unknown stream");
    }

    /**
     * allow to subscribe for infinite/ongoing streams.
     *
     * @param streamId
     * @param <T>
     * @return
     */
    public IPromise<RxPublisher> listen( String streamId ) {
        if ( "TIME".equals(streamId) ) {
            return resolve(timeStream);
        }
        return reject("unknown stream");
    }

    public static void main(String[] args) {

        RxStreamServer server = Actors.AsActor(RxStreamServer.class,256000); // give fat queue
        server.init();
        TCPNIOPublisher publisher = new TCPNIOPublisher(server, 7890);
        publisher.publish(actor -> System.out.println("actor " + actor + " disconnected")).await();

        System.out.println("server started at " + publisher);
    }

}
