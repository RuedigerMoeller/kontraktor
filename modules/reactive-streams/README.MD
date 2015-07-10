# Reactive Streams.remoted

Remotable Implementation of the reactive streams 1.0 spec (http://www.reactive-streams.org/) based on Kontraktor dsitrbuted actors.

* remoting/network bridge for other reactive-streams implementations (no need to move code base to kontraktor)
* translate java 8 streams to reactive-streams
* interoperation of kontraktor actors with other reactive libraries/frameworks
* build highperformance, customizable message queue / stream oriented middleware components combining the power of kontraktor remoting and reactive streams

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

see https://github.com/RuedigerMoeller/kontraktor/tree/trunk/modules/reactive-streams/src/test/java/examples for more example code.

maven coming soon.

##Overview

kontraktor reactive streams contains just a handful of classes.

**`EventSink`** - a starting point of a reactive streams chain providing 'offer' semantics (translates push model to the pull model of reactive streams).

**`KxPublisher`** is a subinterface of the reactive-streams `Publisher` interface. It just adds convenience methods using Java 8's default methods, it does not add a single 'real' interface method to the original `Publisher` interface. One can translate a given `Publisher` implementation (e.g. from RxJava) to a `KxPublisher` calling `KxReactiveStreams.get().asKxPublisher( publisher )`.

**`CancelException`** can be thrown when using Java 8 streams to process a reactive stream. It then translates to a `Subscribtion.cancel` internally. Same applies when consuming a reactive stream using a Kontraktor `Callback`.

**`KxReactiveStreams`** - a singleton (`KxReactiveStreams.get()`) providing a set of conversion and creation methods. With exception of `connect()` which can be used to connect to a remoted reactive stream, one mostly uses `EventSink` and utility API of `KxPublisher`.

###Publishing Streams

**EventSink**

'offer' style starting point. Supports a single subscriber. Implements `KxPublisher` so supports various `subscribe` methods, `map` (synchronous), `asyncMap` (uses separate thread) and `serve(network connector)`

```java
EventSink<Integer> eventSink = new EventSink<Integer>();

eventSink
    .map(i -> i * i)
    .asyncMap(i -> "" + i)
    .subscribe( [some reactive-streams subscriber] );
    
// feeding data:
for ( int i = 0; i < MASMSG_NUM; i++ ) {
    while( ! eventSink.offer(i) ) {
        Thread.yield();
    }
}
```

**Intrinsic Java 8**

`produce()` is available for collections, iterators and java 8 streams.

```java
    KxReactiveStreams.get()
        .produce(IntStream.range(0, MASMSG_NUM))
        .subscribe([some reactive-streams subscriber]);
```

###Consuming reactive Streams
