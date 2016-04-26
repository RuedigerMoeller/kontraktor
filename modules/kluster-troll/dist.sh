#!/usr/bin/env bash
mvn clean package
cp -f ./target/kluster-troll-3.24-jar-with-dependencies.jar ./dist/kkluster.jar