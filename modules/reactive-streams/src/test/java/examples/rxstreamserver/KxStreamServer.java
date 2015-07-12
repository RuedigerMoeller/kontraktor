package examples.rxstreamserver;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.reactivestreams.EventSink;
import org.nustaq.kontraktor.reactivestreams.KxReactiveStreams;
import org.nustaq.kontraktor.reactivestreams.KxPublisher;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.util.Pair;

import java.util.HashMap;
import java.util.stream.IntStream;

/**
 * Created by ruedi on 05/07/15.
 *
 * With "RxSubscriber.serve()", one stream is published on the network on a single address (e.g. localhost:7777).
 * Once the stream is complete,
 * the associated network connection will terminate. Due to network related latency, it takes a while until the address
 * can be reused.
 * Therefore this style of exposing streams is mostly suited for "infinite streams". "onComplete" then
 * actually signals connection close.
 *
 * To allow for multiplexing several streams using a single network connector, kontraktor remoting can be used.
 * Note this is an example on how generic Kontraktor Remoting is leveraged by
 * simply distributing the "local" implementation.
 *
 */
public class KxStreamServer extends Actor<KxStreamServer> {

    EventSink<Long> timeSink;

    // the pair boolean denotes if the stream is infinite
    HashMap<String,Pair<KxPublisher,Boolean>> streams;

    public void init() {
        streams = new HashMap<>();
        timeSink = new EventSink<Long>();
        // EventSink is *not* remoteable,
        // need to create an remoteable async publisher (is actor, so remoteable)
        streams.put("TIME",new Pair(timeSink.map(l -> l),true));
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
    public <T> IPromise<KxPublisher<T>> createStream( String streamId, int start, int end ) {
        if ( "NUMBERS".equals(streamId) ) {
            return new Promise<>((KxPublisher<T>) KxReactiveStreams.get().produce(IntStream.range(start,end)));
        }
        if ( "STRINGS".equals(streamId) ) {
            return new Promise<>((KxPublisher<T>) KxReactiveStreams.get().produce(IntStream.range(start,end)).syncMap(i -> "" + i));
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
    public <T> IPromise<KxPublisher<T>> listen( String streamId ) {
        Pair<KxPublisher, Boolean> pair = streams.get(streamId);
        if ( pair != null ) {
            if ( !pair.cdr() ) // if it is not infinite
            {
                // consume it
                streams.remove(streamId);
            }
            return resolve(pair.car()); // the stream
        }
        return reject("unknown stream");
    }

    /**
     * A client might put a stream such that it can be consumed by other clients (makes this kind of a light weight message queue).
     * if infinite is true, it can be consumed by an arbitrary amount of clients, if false it can be consumed by
     * exactly one client
     *
     * @param id
     * @param pub
     * @param infinite
     */
    public void putStream( String id, KxPublisher pub, boolean infinite ) {
        streams.put(id, new Pair(pub,infinite));
    }

    public static void main(String[] args) {

        KxStreamServer server = Actors.AsActor(KxStreamServer.class,256000); // give fat queue
        server.init();
        TCPNIOPublisher publisher = new TCPNIOPublisher(server, 7890);
        publisher.publish(actor -> System.out.println("actor " + actor + " disconnected")).await();

        System.out.println("server started at " + publisher);
    }

}
