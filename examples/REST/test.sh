#!/usr/bin/env bash
curl -i -X GET http://localhost:8080/api/books/234234
curl -i -X POST --data "param1=value1&param2=value2" http://localhost:8080/api/stuff
curl -i -X POST --data "{ \"key\": \"value\", \"nkey\": 13 }" http://localhost:8080/api/stuff1
curl -i -X PUT --data "{ \"name\": \"satoshi\", \"nkey\": 13345 }" 'http://localhost:8080/api/user/nakamoto/?x=simple&something=pokpokpok&x=13&age=111'