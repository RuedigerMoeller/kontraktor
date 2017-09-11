# Microservice / Krouter example

* How to set up completely isolated, typesafe services
* How to publish and failover / load-balance them using WebSocket based Krouter's

## Krouter

A Krouter is like nginx for remoted actors (=services). 

* Services connect and publish at a Krouter
* Clients connect to Krouter
* Krouter load balances / provides high availability (failover) both stateless and
stateful services

Also possible:
* run/publish multiple api-versions of a service
* run/publish same service with different encodings
* zero-downtime upgrade of a service
* run a service fully redundant (fastest response wins)
* dynamic load balancing (add service instances to a running cluster in order to scale)

## Run

* run build.sh
* start run-krouter.sh
* start several service instances (run-sful-service.sh,run-sless-service.sh)
* start one or more clients
* check reaction if another stateless service is added / removed

currently only WebSocket connections have been tested, should work with tcp connecteable also 
(but only one service type per krouter instance then, no url pathes like websockets).

(K)routing flavors:
* HotCold - from a list of service instances, one is active. Once a service goes down, next one is taken
from the list. Its possible to dynamically add/remove service instances
* Round-Robin - from a list of service instances, requests are dispacthed round robin. Stateful services
are assigned a sticky service instance. Once this goes down, the stateful client is notified of
failover in order to re-establish state (e.g. stream subscriptions/listeners). Its possible to dynamically add/remove service instances
* HotHot - each client request is dispacthed to several service instances - fastest response wins. 
In case of stateful services dynamic addition of service instances is not possible. Its possible to dynamically add/remove service instances
for stateless services.