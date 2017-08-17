usually a distributed actor system expects all nodes to have access to full application classpath.
this example shows how an actor based service can be exposed requiring the client using a empty "Actor Interface" implementation.
The client only needs to have "common-interface" in its dependencies in order to connect to the Service (Actor)