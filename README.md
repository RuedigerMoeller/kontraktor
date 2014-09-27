kontraktor
==========

lightweight and efficient Actor implementation in Java. The threading model implemented has many similarities to node.js, go's and Dart's model of concurrency.

Kontraktor implements a typed actor model to avoid message definition+handling boilerplate code. Additionally this integrates well
with code completion and refactoring of modern IDEs.

Blogposts (kontraktor version > 2.0):

* [Solving "Dining Philosophers problem" with (distributed) actors](http://java-is-the-new-c.blogspot.de/2014/09/breaking-habit-solving-dining.html)


###2.0 beta

* package name change, requires jdk 1.8
* each actor now has dedicated queues, 1.x style scheduling (many actors from one per-thread-queue) caused issues in some scenarios.
* New: Elastic Scheduler scales up actors horizontally by adding cores if needed based on profiling data
* New: Actor Remoting: TCP, HTTP-WebService, WebSockets
* New: Spores
* Streamlined API, added new utils
* added many sanity checks to help spotting actor contract violations for beginners
* Documentation see 2.0-beta wiki page (in progress)

**2.0 beta State:**
* Core Actor functionality stable. 
* TCP remoting also stable, probably issues in corner cases (e.g. dynamically connecting/disconnecting etc.). 
* WebSocket Remoting functionality requires unreleased sub project "netty-kontraktor" (see source).
* Single line WebService actor publishing lacks documentation, zero test coverage (uses kson/json encoding)
* Still unoptimized. Currently: 4-5 million messages per second in-process, ~1.0 million messages per second via TCP-Remoting/fast-serialization 2.x (ofc depends on number of arguments/message size).

```xml
<dependency>
    <groupId>de.ruedigermoeller</groupId>
    <artifactId>kontraktor</artifactId>
    <version>2.0-beta-1</version>
</dependency>
```

###Productive 1.x

Requires JDK 1.7+, but JDK 8 is recommended as readability is much better with lambda's and now optional "final" modifier.

[Documentation](https://github.com/RuedigerMoeller/kontraktor/wiki) is work in progress,

[SampleApp - a nio http 1.0 webserver skeleton done with actors](https://github.com/RuedigerMoeller/kontraktor-samples/tree/master/src/main/java/samples/niohttp)



```xml
<dependency>
    <groupId>de.ruedigermoeller</groupId>
    <artifactId>kontraktor</artifactId>
    <version>1.15</version>
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
up to 9 million messages per second (on i7 laptop, method with 3 arguments passed).


