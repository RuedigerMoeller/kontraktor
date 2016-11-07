#!/usr/bin/env bash
mvn clean package
cp -f ./target/kluster-troll-*-jar-with-dependencies.jar ./dist/kkluster.jar