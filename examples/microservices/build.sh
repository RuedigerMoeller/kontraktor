#!/usr/bin/env bash

cd stateful/interface
mvn clean install
cd ../impl
mvn clean install
cd ../sample-client
mvn clean package
cd ..
cd ..

cd stateless/interface
mvn clean install
cd ../impl
mvn clean install
cd ../sample-client
mvn clean package
cd ..
cd ..
