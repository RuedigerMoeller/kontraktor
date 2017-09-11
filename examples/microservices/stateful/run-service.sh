#!/usr/bin/env bash

java -cp ./impl/target/myservice.jar microservice.impl.MyService -enc bin -url ws://localhost:6667/myservice/v1/bin