#topic based Pub-Sub example 

* websockets, connection based
* uses a mediation service ("Gossip Server"). Need to start first.
* can send messages fire-and-forget to all subscribers of a topic
* supports one-to many request/response. A group member sends one message and receives a reply from each memeber of the group.

show the power of abstraction. Just a few lines of quite naive code:
* no callback id mapping and stuff
* easy implementation request timeout if one member has left the cluster
* no encoding/decoding hassle
