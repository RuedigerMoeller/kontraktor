#!/usr/bin/env bash
mvn package
cp -f ./target/kontraktor*dependencies.jar ./dist/kkluster.jar