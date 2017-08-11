#kontraktor-kluster

a simple util to manage processes in a distributed environment.

A cluster is defined by a peer to peer network of demon processes. The client ("troll.sh" from german 'kontrolle')
can connect to any of those demons and monitor, start and stop processes on all nodes making up the p2p network.

A hosted distributed java application is able to control itself by connecting directly via kontraktor
remoting (see implementation of client).

tested on linux only, porting will require some adaption of process and path handling.
