#!/usr/bin/env bash

cd babelserver
npm publish
cd ..

cd js4k
npm publish
cd ..

cd kontraktor-common
npm publish
cd ..

cd kontraktor-client
npm publish
cd ..

cd kontraktor-server
npm publish
cd ..