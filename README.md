#kontraktor 4


##What is kontraktor ?

kontraktor is 

* an implementation of the actor-model using a node-js'ish single thread event loop approach to implement actors for Java. Ofc its possible to run many Actors (utilizing more than one core) in a
single Java process.
* main focus is 'distributed actors'. This means actors can talk over-the-wire to each other
* kontraktor's remote-invocation can be used using Http(s) / WebSockets / TCP, messages are encoded using binary serialization or json.
* switching the network protocol or encoding can be done dynamically, no need to change actual application code
* kontraktor implements a highly sophisticated cross language javascript<=>java communication using a general approach. E.g. Transparent Callbacks/Promises. 
* fully asynchronous
* high performance

things you can do using kontraktor

* Create nodejs-style Java Backends for javascript written Single Page / progressive WebApps using the advanced interop (java<=>javascript) mechanics of kontraktor
* Slice your backend into several independent services/process without much effort. Because of the actor abstraction, one still can choose to run a
 cluster inside a single process if wanted. (Physical deployment is not hard-wired into your app)
* write a service in nodejs / javascript and connect it from a java process
* connect a java service from a nodejs-app
* replace Java's synchronous, shared state concurrency model by a shared-nothing asynchronous model (actors instead threads). 

##Modules

Kontraktor consists of several modules. For sake of simplicity all module versions are kept in sync with kontraktor core.

###Kontraktor Core 

Actors + TCP Remoting), requires Java 8, LGPL

* transform regular java code (satisfying some conventions) into remoteable actors.
* no boilerplate like message classes etc. required
* TCP remoting included (2 implementations: SyncIO and AsyncIO) 

```xml
<dependency>
    <groupId>de.ruedigermoeller</groupId>
    <artifactId>kontraktor</artifactId>
    <version>4.00</version>
</dependency>
```

###Kontraktor Http 

Ads WebSockets, Http LongPoll for actor-remoting, JavaScript interop, requires Java 8, LGPL. Uses Undertow as underlying webserver

* npm modules to **(a)** implement a kontraktor actor (=service) using nodejs and **(b)** to connect a kontraktor service from nodejs 
* server push via adaptive longpolling (polling automatically turns off if no open callback / future is present) 
* alternatively use websockets
* transparent batching of client requests. If a client uses many remote calls concurrently, kontraktor automatically batches those calls and their repsonses
into a single http request/response.
* advanced bundling and inlining of ressources (js, css, html) webpack style. Instead of introducing a build step, kontraktor bundles
 your stuff dynamically upon first request to your server (when in production mode). This reduces file clutter and allows to tweak/hotfix your app
 in-place deployed.

```xml
<dependency>
    <groupId>de.ruedigermoeller</groupId>
    <artifactId>kontraktor-http</artifactId>
    <version>4.00</version>
</dependency>
```

####npm (kontraktor-http)

**kontraktor-common**

defines fundamentals: remote actor refs, en/decoding of Java-serialized objects, KPromise

**kontraktor-client**

Can be used from a browser (attention then: needs to be added using a <script> tag, not babel/browserify'ish using 'require').
Can be used from nodejs to connect services/actors implemented in java or javascript

**kontraktor-server**

write an ES6 class and make it accessible to other (kontraktor) processes using websockets. Some limitations: no actor proxies, only websockets supported.

**js4k**

old (es6 free) implementation of kontraktor-client. somewhat messy, but production-proven
 
###kontraktor-web

A lightweight framework on top of kontraktor to serve JavaScript Single Page Application clients (e.g. Polymer.js, React.js) from a Java Server.

* session handling: for each client an actor instance is created server side. No need to manually juggle Id's
* session invalidation
* session resurrection (=wake up / re-establish a session from a Client which has been away for some time). No more "your session has expired")
* built in support for Polymer.js and React.js (incl. jsx, babel+browserify) 

```xml
<dependency>
    <groupId>de.ruedigermoeller</groupId>
    <artifactId>kontraktor-http</artifactId>
    <version>4.00</version>
</dependency>
```

**[Undocumented] kontraktor-reallive**

A clusterable NoSQL data base.

* Reactive: Each transaction/data change is broadcasted to (filtered) subscribers. CQRS/MVC at cluster scale.
* Can be used to organize intra-service communication in a fail-safe and decoupled fashion.
* clusterable
* optional full in memory caching. 
* distributed Lambda execution featuring advanced and very fast inmemory analytics.
* powers message routing middleware of a large european stock exchange (up to 100.000 transactions per second, up to 200k messages per second)
* powers realtime NLP and cluster-coordination of juptr.io

Currently undocumented

**[Undocumented, in-progress] service-support**

* framework/tools to control and configure a cluster of reallive + webserver(s) + (micro-)services

**[Undocumented] cluster-troll**

Simple peer-2-peer network of "Process Controlling" nodes. Enables to start/stop clusters in a distributed setup (several machines).

```xml
<dependency>
    <groupId>de.ruedigermoeller</groupId>
    <artifactId>kontraktor-http</artifactId>
    <version>4.00</version>
</dependency>
```

**[Untested] Kontraktor-Reactive Streams** 

(Implements Reactive Streams Spec 1.0), LGPL

```xml
<dependency>
    <groupId>de.ruedigermoeller</groupId>
    <artifactId>kontraktor-reactive-streams</artifactId>
    <version>4.00</version>
</dependency>
```

**[Untested] Kontraktor-Bare** 

(Minimalistic standalone Http-LongPoll client [legacy apps, Android] ), requires Java 7, Apache 2.0 Licensed

```xml
<dependency>
    <groupId>de.ruedigermoeller</groupId>
    <artifactId>kontraktor-bare</artifactId>
    <version>4.00</version>
</dependency>
```

###Examples:
https://github.com/RuedigerMoeller/kontraktor/tree/trunk/examples


Older Blogposts (samples are of *OLD* 2.0, 3.0 version, might need rewrite/changes (mostly `Future` => `IPromise`):

* http://java-is-the-new-c.blogspot.de/2015/07/polymer-webcomponents-served-with-java.html
* [Solving "Dining Philosophers problem" with (distributed) actors](http://java-is-the-new-c.blogspot.de/2014/09/breaking-habit-solving-dining.html)
* [A persistent KeyValue Server in 40 lines and a sad fact](http://java-is-the-new-c.blogspot.de/2014/12/a-persistent-keyvalue-server-in-40.html)
* [Alternatives to Executors when scheduling Tasks/Actors](http://java-is-the-new-c.blogspot.de/2014/10/alternatives-to-executors-when.html)
