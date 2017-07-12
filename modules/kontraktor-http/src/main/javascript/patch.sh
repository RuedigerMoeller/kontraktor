#!/usr/bin/env bash

cd babelserver
npm version patch
cd ..

cd js4k
npm version patch
cd ..

cd kontraktor-common
npm version patch
cd ..

cd kontraktor-client
npm version patch
cd ..

cd kontraktor-server
npm version patch
cd ..