#!/usr/bin/env bash

java -cp ./impl/target/stateless.jar microservice.impl.StatelessService -url ws://localhost:6667/slservice/v1/bin -url ws://localhost:6667/slservice/v1/bin