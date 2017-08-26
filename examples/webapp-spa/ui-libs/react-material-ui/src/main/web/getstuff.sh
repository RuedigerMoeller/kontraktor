#!/usr/bin/env bash

npm i react react-dom material-ui

# gets latest version from unpkg.com. adapt urls for a fixed version
# take care, we need browser compatible builds (umd)

# kontraktor
wget -O lib/kontraktor-common.js https://unpkg.com/kontraktor-common/kontraktor-common.js
wget -O lib/kontraktor-client.js https://unpkg.com/kontraktor-client/kontraktor-client.js
