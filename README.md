kontraktor 3
============

high performance, lightweight and boilerplate free distributed eventloop'ish Actor implementation designed for Java 8.

http://ruedigermoeller.github.io/kontraktor/

**3.0** is a major upstep to previous versions.

**3.0 documentation**

check [wiki](https://github.com/RuedigerMoeller/kontraktor/wiki/Kontraktor-3).

**Maven**

Kontraktor Core (Actors + TCP Remoting), requires Java 8, LGPL Licensed
```xml
<dependency>
    <groupId>de.ruedigermoeller</groupId>
    <artifactId>kontraktor</artifactId>
    <version>3.00</version>
</dependency>
```

Kontraktor Http (WebSockets, Http LongPoll, Single Page App + JavaScript interop support), requires Java 8, LGPL Licensed
```xml
<dependency>
    <groupId>de.ruedigermoeller</groupId>
    <artifactId>kontraktor-http</artifactId>
    <version>3.00</version>
</dependency>
```

Kontraktor-Bare (Minimalistic standalone Http-LongPoll client [legacy apps, Android] ), requires **>=Java 7**, Apache 2.0 Licensed
```xml
<dependency>
    <groupId>de.ruedigermoeller</groupId>
    <artifactId>kontraktor-bare</artifactId>
    <version>3.00</version>
</dependency>
```

### what changed compared to 2.0 ?

* simplified thread scheduling model. An Actor has a fixed assigned thread. Many actors can be scheduled on the same thread explicitely, no "magic" auto-scaling + pool execution. In the end its important to know what's going on, especially when mixing actors and idiomatic java style block-all-the-threads concurrency.
* rewrote and redesigned remoting layer such that a minimum of transport specific code is required. Reduces errors as most part of remoting logic is shared accross all transport options (tcp, tcp nio, websocket, http long poll, fst-serialization, json-serialization).
* changed naming of concurrency primitives EcmaScript 6/7 style. In 2.0 `Future` clashed with JDK, so now `Future` became `IPromise`. Also concurrency tooling has been renamed ES6/7 alike: `race`, `all`, `yield`, `await`. As the concurrency model is similar to Node.js (difference: we can run N instances, we have threads :) ), this seemed the best option to avoid alienation, at the same time prevent name clashes with java.util.concurrent.
* JavaScript + SPA interop

Old Blogposts (samples are of *OLD* 2.0 version, would need minor rewrite (mostly `Future` => `IPromise`):

* [Solving "Dining Philosophers problem" with (distributed) actors](http://java-is-the-new-c.blogspot.de/2014/09/breaking-habit-solving-dining.html)
* [A persistent KeyValue Server in 40 lines and a sad fact](http://java-is-the-new-c.blogspot.de/2014/12/a-persistent-keyvalue-server-in-40.html)
* [Alternatives to Executors when scheduling Tasks/Actors](http://java-is-the-new-c.blogspot.de/2014/10/alternatives-to-executors-when.html)
