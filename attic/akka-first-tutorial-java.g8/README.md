A [giter8] based [Akka] 2.0 tutorial project using [Java] and [Maven].

Prerequisites to use this tutorial is to have [Java], [Maven] and [giter8] installed.

The next step is to open a terminal window and:
	
	$ cd /Users/theuser/code (or where ever you would like your code to end up)
	$ g8 typesafehub/akka-first-tutorial-java
    
	Akka 2.0 Pi Calculation Tutorial with Java and Maven

	name [Akka Pi Calculation Tutorial]:
	group_id [org.example]:
	artifact_id [pi-calculation]:
	package [org.example]:
	version [0.1-SNAPSHOT]:
	akka_version [2.0]:

	Applied typesafehub/akka-first-tutorial-java.g8 in akka-pi-calculation-tutorial

Okay, so now you have created the tutorial as a local project on your computer.
To run and test it use [Maven]:

	$ cd akka-pi-calculation-tutorial
	$ mvn compile exec:java
	[INFO] Scanning for projects...
	[INFO]                                                                         
	[INFO] ------------------------------------------------------------------------
	[INFO] Building Akka Pi Calculation Tutorial 0.1-SNAPSHOT
	[INFO] ------------------------------------------------------------------------
	[INFO] 
	[INFO] --- maven-resources-plugin:2.5:resources (default-resources) @ pi-calculation ---
	[debug] execute contextualize
	[INFO] skip non existing resourceDirectory /Users/theuser/code/akka-pi-calculation-tutorial/src/main/resources
	[INFO] 
	[INFO] --- maven-compiler-plugin:2.3.2:compile (default-compile) @ pi-calculation ---
	[WARNING] File encoding has not been set, using platform encoding MacRoman, i.e. build is platform dependent!
	[INFO] Compiling 1 source file to /Users/theuser/code/akka-pi-calculation-tutorial/target/classes
	[INFO] 
	[INFO] >>> exec-maven-plugin:1.2.1:java (default-cli) @ pi-calculation >>>
	[INFO] 
	[INFO] <<< exec-maven-plugin:1.2.1:java (default-cli) @ pi-calculation <<<
	[INFO] 
	[INFO] --- exec-maven-plugin:1.2.1:java (default-cli) @ pi-calculation ---
	    Pi approximation:    3.1415926435897883
	    Calculation time:    565 milliseconds
	[INFO] ------------------------------------------------------------------------
	[INFO] BUILD SUCCESS
	[INFO] ------------------------------------------------------------------------
	[INFO] Total time: 3.027s
	[INFO] Finished at: Thu Mar 08 21:03:48 CET 2012
	[INFO] Final Memory: 9M/81M
	[INFO] ------------------------------------------------------------------------

License
-------

Copyright 2012 Typesafe, Inc.

Licensed under the [Apache License, Version 2.0][apache2] (the "License"); you
may not use this software except in compliance with the License. You may obtain
a copy of the License at:

[http://www.apache.org/licenses/LICENSE-2.0][apache2]

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.

[giter8]: https://github.com/n8han/giter8
[Akka]: http://akka.io
[Java]: http://java.com/
[Maven]: http://maven.apache.org/
[apache2]: http://www.apache.org/licenses/LICENSE-2.0
