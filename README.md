# kontraktor 4

## What is kontraktor ?

* A boilerplate free and consistent abstraction for asynchronous remote communication powered by a **distributed actor-model**
* One important application of this abstraction are **SPA WebApplications** enabling Java to back modern SPA javascript client frameworks like React.js, Polymer.js, .. (transpilation, packaging, optimization). Different to webpack/browserify etc. kontraktor does this on the fly (when running in prod mode) without an extra build step using pure java. [see IntrinsicReact example](https://github.com/RuedigerMoeller/InstrinsicReactJSX)
* separates network transport (TCP, WebSockets, Http) and message encoding (fast-serialization, json, ..) from application code. 
* **asynchronous, non-blocking,high performance**
* production proven

[Documentation](https://github.com/RuedigerMoeller/kontraktor/wiki)

[Code Examples](https://github.com/RuedigerMoeller/kontraktor/tree/trunk/examples), [Related Blogpost's](https://juptr.io/@kontraktor)

[React Example](https://github.com/RuedigerMoeller/InstrinsicReactJSX)

## What can I use it for ?

* build modern microservice systems (java (JVM), nodejs) beyond the Limits of REST. Asynchronous, event sourced, independent of encoding and network transport (choose from TCP, WebSockets, Http-(Long Poll), json or binary encoding).
* power modern SPA apps with a java based webserver

**Credits**: kontraktor makes use of many awesome open source libraries such as
* undertow, (io.undertow) see [undertow.io](undertow.io)
* jsoup (org.jsoup) - real world html parser
* httpasyncclient (org.apache.httpcomponents) 
* npm-semver, (com.github.yuchi)
* fast-classpath-scanner (io.github.lukehutch)
* org.apache.commons, slf4j-api, minimal-json (com.eclipsesource.minimal-json), junit


## Modules

Kontraktor consists of several modules. For sake of simplicity all module versions are kept in sync with kontraktor core.

### Kontraktor Core 

Actors + TCP Remoting

* transform regular java code (satisfying some conventions) into remoteable actors.
* no boilerplate required
* TCP remoting included (2 implementations: SyncIO and AsyncIO) 
* general messaging optimizations: batching, binary queues (reduce GC load + save heap by queuing raw bytes instead of object's).

**[Core Documentation](https://github.com/RuedigerMoeller/kontraktor/wiki/Kontraktor-4-Core)** [in progress]

```xml
<dependency>
    <groupId>de.ruedigermoeller</groupId>
    <artifactId>kontraktor</artifactId>
    <version>4.22</version>
</dependency>
```

### Kontraktor Http 

Adds WebSockets, Http LongPoll for actor-remoting, JavaScript interop. Uses Undertow as underlying webserver

* npm modules to **(a)** implement a kontraktor actor (=service) using nodejs and **(b)** to connect a kontraktor service from nodejs 
* server push via adaptive longpolling (polling automatically turns off if no pending callback / promise is present) 
* support for websockets
* advanced bundling and inlining of resources (js, css, html) webpack style. Instead of introducing a build step, kontraktor bundles and caches your stuff dynamically upon first request (production mode). 
* session handling fundamentals

**[JavaScript, Http-Documentation](https://github.com/RuedigerMoeller/kontraktor/wiki/Kontraktor-4-Http)** [in progress]

**[Instrinsic React/JSX Documentation](https://github.com/RuedigerMoeller/kontraktor/wiki/Kontraktor-4-React-JSX)** [in progress]

```xml
<dependency>
    <groupId>de.ruedigermoeller</groupId>
    <artifactId>kontraktor-http</artifactId>
    <version>4.22</version>
</dependency>
```

### kontraktor-http 4 javascript nodejs. npm modules

**kontraktor-common**

defines fundamentals: remote actor refs, en/decoding of Java-serialized objects, KPromise

**kontraktor-client**

Can be used from a browser (attention then: needs to be added using a <script> tag, not babel/browserify'ish using 'require').
Can be used from nodejs to connect services/actors implemented in java or javascript

**kontraktor-server**

write an ES6 class and make it accessible to other (kontraktor) processes using websockets. Some limitations: no actor proxies, only websockets supported.

**js4k**

old (es5) implementation of kontraktor-client. somewhat messy, but production-proven
 
**Kontraktor-Bare** 

(Minimalistic standalone Http-LongPoll client [legacy apps, Android] ), requires Java 7, Apache 2.0 Licensed

```xml
<dependency>
    <groupId>de.ruedigermoeller</groupId>
    <artifactId>kontraktor-bare</artifactId>
    <version>4.22</version>
</dependency>
```

### Examples:
https://github.com/RuedigerMoeller/kontraktor/tree/trunk/examples

### Misc
Older Blogposts (samples are of *OLD* 2.0, 3.0 version, might need rewrite/changes (mostly `Future` => `IPromise`):

* http://java-is-the-new-c.blogspot.de/2015/07/polymer-webcomponents-served-with-java.html
* [Solving "Dining Philosophers problem" with (distributed) actors](http://java-is-the-new-c.blogspot.de/2014/09/breaking-habit-solving-dining.html)
* [A persistent KeyValue Server in 40 lines and a sad fact](http://java-is-the-new-c.blogspot.de/2014/12/a-persistent-keyvalue-server-in-40.html)
* [Alternatives to Executors when scheduling Tasks/Actors](http://java-is-the-new-c.blogspot.de/2014/10/alternatives-to-executors-when.html)
