# Reactive Streams.remoted

* leverages Kontraktor's high performance generic remoting to provide Remotable Reactive Streams
* does not require to move into Kontraktor, can be used as a pure remoting bridge
* maps reactive streams to java 8 streams

Example of publishing and consuming a RxJava Stream using Kontraktor as a remote brige.

```java
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
        .forEach( i -> rm.count() );
}
```

does ~1.3 million events per second over tcp nio. 1.45 million over WebSocket.

see src/test/examples for more example code

maven coming soon.
