# Reactive Streams.remoted

Remotable Implementation of the reactive streams 1.0 spec (http://www.reactive-streams.org/) based on Kontraktor (distributed) actors.

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

###Publishing reactive streams

**EventSink**

'offer' style starting point. Supports a single subscriber. Implements `KxPublisher` so supports various `subscribe` methods, `syncMap` (synchronous), `map` (uses separate thread) and `serve(network connector)`

```java
EventSink<Integer> eventSink = new EventSink<Integer>();

eventSink
    .syncMap(i -> i * i)
    .map(i -> "" + i)
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

>**`map(),async()`** create a 1:N publisher allowing for an arbitrary number of subscribers. Backpressure will be processed such that the slowest consumer determines throughput for all [more advanced policies to be added in future releases]

>**`syncMap()`+`EventSink`** support a single subscriber only.

If a processor maps to 'null' this is treated as event suppression. 

```java
    KxReactiveStreams.get()
        .produce(IntStream.range(0, MASMSG_NUM))
        .syncMap( i -> { return i%1 == 0 ? null : i; } )
        .subscribe([some reactive-streams subscriber]);
```

###Consuming reactive streams

**Intrinsic Kontraktor**

In addition inside a Kontraktor actor execution context, callbacks/streams/iterator's run inside the same thread, so its unnecessary to care for shared data (see https://github.com/RuedigerMoeller/kontraktor/blob/trunk/modules/reactive-streams/src/test/java/examples/KontraktorStreams.java for more examples).

`reactive-streams.Subscriber` is mapped to kontraktor's `callback()`. One has to check the 'error' object in order to determine wether the event is an error (=onError), complete (=onComplete) or a regular event (=onNext). Reason to map this is the capability of Kontraktor to transparently remote `Callback` instances. 

In general its favourable to use Java 8 streaming for efficient in-process event processing as the java 8 stream pipeline is rich, sophisticated and well performing. It does not make sense to double this functionality in Kontraktor. 

**Note:** As always with Kontraktor, if code runs inside an actor, nonblocking primitives are used internally. If code is executed from plain old java threads, the executing thread gets blocked if necessary.

```java
KxReactiveStreams.get().asKxPublisher([reactive-streams publisher])
    .subscribe((event, error) -> {
        if (Actors.isResult(error)) {
            [processing]
        } else if ( Actors.isError(error) ) )
            [error handling]
        } else { // if ( Actors.isComplete(error)
            [done]
            // throw CancelException.Instance to cancel subscription
        }
    });
```

**Instrinsic Java 8**

Example on how to consume a RxJava reactive-stream (see https://github.com/ReactiveX/RxJava) with Java 8
stream API (also possible to make an `Iterator` from a reactive-stream).

```java
// a RxJava reactive-streams publisher
Publisher<Integer> pub = RxReactiveStreams.toPublisher(Observable.range(0, NUM_MSG/4));

AtomicInteger count = new AtomicInteger(0);
Promise<Integer> finished = new Promise<>();
KxReactiveStreams.get().asKxPublisher(pub)
    .async() // caveat: required as RxPublisher is syncronous
    .stream( stream -> {
        stream.forEach(i -> {
            count.incrementAndGet();
            // throw CancelException.Instance to cancel subscription
        });
        finished.resolve(count.get());
    });

Assert.assertTrue(finished.await(50000).intValue() == NUM_MSG/4);
```

As java 8 streams/iterators use a pure `pull-model`, implicit thread creation and blocking is applied where necessary.

**Note:** it's currently not possible to react to errors sent by a reactive-stream when using a java 8 stream for iteration. The stream will just stop. When using `Iterator` to consume a reactive stream, next will throw the original reactive-streams `onError` exception in case.

###Remoting reactive streams

Kontraktor reactive stream internally executes the full reactive streams backpressure protocol incl. `require(N)`, `onNext()` also when remoted as its basically a local implementation remoted by generic kontraktor remoting.

There are two variants on how to expose streams on a network transport:
* Simple: publish a single stream on a network transport (tcp, websocket, binary or json). If the stream completes/has error / is canceled, the underlying connection will close automatically. Downside is that only one stream can be exposed on a transport endpoint this way.
* Advanced: Create a kontraktor remoted actor/service which then exposes reactive streams using generic kontraktor remoting. This enables more advanced logic such as entitlement or client specific stream differences. In addition its possible to multiplex an arbitrary amount of reactive streams to N clients from a single network address.


