kontraktor
==========

lightweight and efficient Actor implementation in Java. The threading model implemented has many similarities to node.js, go's and Dart's model of concurrency.

* **boilerplate free**. No need for handcrafted message dispatch, no need for a definition of "Message" classes or Actor-Interfaces. Full typesafety integrates well with code completion and refactoring features of modern IDEs
* **no** instrumentation agent or post compilation task required

Kontraktor can be used as a model to deal with **concurrency and parallelism**, however its perfectly valid to just make use of Kontraktors **remoting** to ease creation of distributed 'Microservice' alike application topologies.

**2.0 documentation**

check wiki.

Blogposts:

* [Solving "Dining Philosophers problem" with (distributed) actors](http://java-is-the-new-c.blogspot.de/2014/09/breaking-habit-solving-dining.html)
* [A persistent KeyValue Server in 40 lines and a sad fact](http://java-is-the-new-c.blogspot.de/2014/12/a-persistent-keyvalue-server-in-40.html)
* [Alternatives to Executors when scheduling Tasks/Actors](http://java-is-the-new-c.blogspot.de/2014/10/alternatives-to-executors-when.html)


```xml
<dependency>
    <groupId>de.ruedigermoeller</groupId>
    <artifactId>kontraktor</artifactId>
    <version>2.0-beta-4</version>
</dependency>
```


