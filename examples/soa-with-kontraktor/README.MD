# SOA cluster example

This example comes without database and data nodes. In order to add DataNode, 
remove autobalancing call in MyRegistry::main. Trigger balance via localhost:1113/mon/balance 
then instead.

