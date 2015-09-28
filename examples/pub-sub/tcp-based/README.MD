#topic based Pub-Sub example 

* websockets, connection based
* uses a mediation service ("Gossip Server"). Need to start first.
* can send messages fire-and-forget to all subscribers of a topic
* supports one-to many request/response. A group member sends one message and receives a reply from each subscriber of the topic.

show the power of abstraction. Just a few lines of quite naive code:
* no callback id mapping and stuff
* easy implementation of request timeout if a member has left the cluster
* no encoding/decoding hassle

`MediationActor` - the pub-sub server

`ReceiverActor` - a pub sub member sending and receiving on a topic

`BroadcastWithResultExample` - a member sending a request to all other members and receives a result from each
