package examples.rxjava;


import org.nustaq.kontraktor.reactivestreams.KxPublisher;
import org.nustaq.kontraktor.reactivestreams.KxReactiveStreams;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;
import org.nustaq.kontraktor.remoting.websockets.WebSocketPublisher;
import org.nustaq.kontraktor.util.RateMeasure;
import org.reactivestreams.Publisher;
import rx.Observable;
import rx.RxReactiveStreams;

/**
 * Created by ruedi on 07/07/15.
 */
public class RxJava {

    public void simpleTest() {
        Observable<Integer> range = Observable.range(0, 50_000_000);
        Publisher<Integer> pub = RxReactiveStreams.toPublisher(range);

        RateMeasure rm = new RateMeasure("events");
        KxReactiveStreams.get().asRxPublisher(pub)
            .subscribe((r, e) -> {
                rm.count();
            });
    }

    public static void remotingTest() {
        Observable<Integer> range = Observable.range(0, 50_000_000);
        Publisher<Integer> pub = RxReactiveStreams.toPublisher(range);

        KxReactiveStreams.get().asRxPublisher(pub)
            .serve(new TCPNIOPublisher().port(3456));

        RateMeasure rm = new RateMeasure("events");
        KxReactiveStreams.get()
            .connect(Integer.class, new TCPConnectable().host("localhost").port(3456))
            .subscribe((r, e) -> {
                rm.count();
            });
    }

    public static void remotingToJ8Streams() {
        Observable<Integer> range = Observable.range(0, 50_000_000);
        Publisher<Integer> pub = RxReactiveStreams.toPublisher(range);

        KxReactiveStreams.get().asRxPublisher(pub)
            .serve(new TCPNIOPublisher().port(3456));

        RateMeasure rm = new RateMeasure("events");
        KxReactiveStreams.get()
            .connect(Integer.class, new TCPConnectable().host("localhost").port(3456))
            .stream( stream -> {
                long count =
                    stream
                        .map(i -> {
                            rm.count();
                            return i;
                        })
                        .count();
                System.out.println("count: "+count);
            });
    }

    public static void remotingRxToRx() {
        Observable<Integer> range = Observable.range(0, 50_000_000);
        Publisher<Integer> pub = RxReactiveStreams.toPublisher(range);

        KxReactiveStreams.get().asRxPublisher(pub)
            .serve(new TCPNIOPublisher().port(3456));

        RateMeasure rm = new RateMeasure("events");

        KxPublisher<Integer> remoteStream =
            KxReactiveStreams.get()
                .connect(Integer.class, new TCPConnectable().host("localhost").port(3456));

        RxReactiveStreams.toObservable(remoteStream)
            .forEach(i -> rm.count());
    }

    public static void remotingRxToRxWebSocket() {
        Observable<Integer> range = Observable.range(0, 50_000_000);
        Publisher<Integer> pub = RxReactiveStreams.toPublisher(range);

        KxReactiveStreams.get().asRxPublisher(pub)
            .serve(new WebSocketPublisher().hostName("localhost").port(7777).urlPath("/ws/rx"));

        RateMeasure rm = new RateMeasure("events");

        KxPublisher<Integer> remoteStream =
            KxReactiveStreams.get()
                .connect(Integer.class, new WebSocketConnectable().url("ws://localhost:7777/ws/rx"));

        RxReactiveStreams.toObservable(remoteStream)
            .forEach( i -> rm.count() );
    }

    public static void main(String[] args) {
        remotingRxToRxWebSocket();
    }



}
