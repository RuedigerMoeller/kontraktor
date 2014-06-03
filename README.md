kontraktor
==========

lightweight and efficient Actor/(CSP) implementation in Java. The threading model implemented has many similarities to node.js, go's and Dart's model of concurrency.

Kontraktor implements a typed actor model to avoid message definition+handling boilerplate code. Additionally this integrates well
with code completion and refactoring of modern IDEs.

Requires JDK 1.7+, but JDK 8 is recommended as readability is much better with lambda's and now optional "final" modifier.

[Full Documentation](https://github.com/RuedigerMoeller/kontraktor/wiki/Kontraktor-documentation) is work in progress,

[Solutions to typical problems of async concurrency](https://github.com/RuedigerMoeller/kontraktor/wiki/Solutions-to-typical-problems-of-async-concurrency)

[SampleApp - a nio http 1.0 webserver skeleton done with actors](https://github.com/RuedigerMoeller/kontraktor-samples/tree/master/src/main/java/samples/niohttp)



```xml
<dependency>
    <groupId>de.ruedigermoeller</groupId>
    <artifactId>kontraktor</artifactId>
    <version>LATEST</version>
</dependency>
```

Kontraktor uses runtime-generated (javassist) proxy instances which put all calls to the proxy onto a queue. A DispatcherThread then dequeues method invocations (=messages) ensuring single-threadedness of actor execution.


E.g.

```java
    public static class BenchSub extends Actor<BenchSub> {
        int count;
        
        public void benchCall(String a, String b, String c) {
            count++;
        }
          
        public Future<Integer> getResult() {
            return new Promise(count);
        }
    }

    public static main(..) 
    {
        final BenchSub bsProxy = Actors.SpawnActor(BenchSub.class); // create proxy + actor instance
        for (int i : new int[10] ) {
            bsProxy.benchCall("u", "o", null); // actually enqueues
        }
        // all communication is async
        bsProxy.getResult().then( (res,err) -> bs.stop() );
    }
```

Kontrakor internally uses a high performance bounded queue implementation of the Jaq-In-A-Box project and can pass 
9 million messages per second (on i7 laptop, method with 3 arguments passed) up to 15 million on decent hardware.


