kontraktor 3
============

high performance, lightweight and boilerplate free distributed eventloop'ish Actor implementation designed for Java 8.

Kontraktor enables you to abstract away the network layer used. So any server/service built with kontraktor can be made available via TCP (blocking or NIO) out of the box, WebSosckets or Http Long Poll. Either with binary serialization or JSon encoding.

JavaScript interop + webserver specialized ond SPA's included.

Kontraktor is high performance, you can do up to 2 million point2point async remote calls per second over tcp.

WebSite: http://ruedigermoeller.github.io/kontraktor/

[**3.0 documentation**](https://github.com/RuedigerMoeller/kontraktor/wiki/Kontraktor-3).

**Maven**

see http://ruedigermoeller.github.io/kontraktor/#maven

### what changed compared to 2.0 ?

* simplified thread scheduling model. An Actor has a fixed assigned thread. Many actors can be scheduled on the same thread explicitely, no "magic" auto-scaling + pool execution. In the end its important to know what's going on, especially when mixing actors and idiomatic java style block-all-the-threads concurrency.
* rewrote and redesigned remoting layer such that a minimum of transport specific code is required. Reduces errors as most part of remoting logic is shared accross all transport options (tcp, tcp nio, websocket, http long poll, fst-serialization, json-serialization).
* changed naming of concurrency primitives EcmaScript 6/7 style. In 2.0 `Future` clashed with JDK, so now `Future` became `IPromise`. Also concurrency tooling has been renamed ES6/7 alike: `race`, `all`, `yield`, `await`. As the concurrency model is similar to Node.js (difference: we can run N instances, we have threads :) ), this seemed the best option to avoid alienation, at the same time prevent name clashes with java.util.concurrent.
* JavaScript + SPA interop

Old Blogposts (samples are of *OLD* 2.0 version, would need minor rewrite (mostly `Future` => `IPromise`):

* [Solving "Dining Philosophers problem" with (distributed) actors](http://java-is-the-new-c.blogspot.de/2014/09/breaking-habit-solving-dining.html)
* [A persistent KeyValue Server in 40 lines and a sad fact](http://java-is-the-new-c.blogspot.de/2014/12/a-persistent-keyvalue-server-in-40.html)
* [Alternatives to Executors when scheduling Tasks/Actors](http://java-is-the-new-c.blogspot.de/2014/10/alternatives-to-executors-when.html)
