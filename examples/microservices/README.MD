# Microservice / Krouter example

* How to set up completely isolated, typesafe services
* How to publish and failover / load-balance them using WebSocket based Krouter's

## Krouter

A Krouter is like nginx for remoted actors. 

* Services connect and publish at a Krouter
* Clients connect to Krouter
* Krouter load balances / provides high availability (failover) both stateless and
stateful services

Also possible:
* run/publish multiple versions of a service
* run/publish same service with different encodings
* zero-downtime upgrade of a service
* run a service fully redundant (fasted response wins)
* dynamic load balancing (add service instances to a running cluster in order to scale)

## Run

* run build.sh
* start run-krouter.sh
* start several service instances (run-sful-service.sh,run-sless-service.sh)
* start one or more clients
* check reaction if another stateless service is added / removed