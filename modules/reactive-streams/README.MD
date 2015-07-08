# Reactive Streams.remoted

* enables interop of kontraktor with reactive streams
* leverages Kontraktor's high performance generic remoting to provide Remotable Reactive Streams
* can be used as a pure remoting bridge, does not require to move existing codebase to Kontraktor
* optionally maps reactive streams to java 8 streams (=> remotable Java 8 streams)

Example of publishing and consuming a RxJava Stream using Kontraktor as a remote bridge over WebSockets.

```java
public static void remotingRxToRxWebSocket() {
    Observable<Integer> range = Observable.range(0, 50_000_000);
    Publisher<Integer> pub = RxReactiveStreams.toPublisher(range);

    KxReactiveStreams.get().asKxPublisher(pub)
        .serve(new WebSocketPublisher().hostName("localhost").port(7777).urlPath("/ws/rx"));

    RateMeasure rm = new RateMeasure("events");

    KxPublisher<Integer> remoteStream =
        KxReactiveStreams.get()
            .connect(Integer.class, new WebSocketConnectable().url("ws://localhost:7777/ws/rx"));

    RxReactiveStreams.toObservable(remoteStream)
        .forEach( i -> rm.count() );
}
```
does ~1.3 million events per second over tcp nio. 1.45 million over WebSocket.

example of streaming Java 8 Streams over tcp:

```java
public <T> void remoteJava8Streams( Function<MyEvent,T> doStuff ) throws InterruptedException {
    KxReactiveStreams.get()
        .produce(IntStream.range(0, 5_000_000).mapToObj(i -> new MyEvent(i,Math.random(),"Hello"+i)))
        .serve( new TCPPublisher().port(8123) );

    CountDownLatch latch = new CountDownLatch(1);

    RateMeasure measure = new RateMeasure("rate");
    KxReactiveStreams.get()
        .connect( MyEvent.class, new TCPConnectable().host("localhost").port(8123) )
        .stream( stream -> {
            long count = stream
                .map( event -> {
                    measure.count();
                    return doStuff.apply(event);
                })
                .count();

            System.out.println("Count:"+count);
            latch.countDown();
        });

    latch.await();
}
```

see src/test/examples for more example code

maven coming soon.
