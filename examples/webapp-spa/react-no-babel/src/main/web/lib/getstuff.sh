#!/usr/bin/env bash

# gets latest version. adapt urls for a fixed version
wget -O react.js https://unpkg.com/react/dist/react.js
wget -O react-dom.js https://unpkg.com/react-dom/dist/react-dom.js
wget -O react-router.js https://unpkg.com/react-router/umd/react-router.js
wget -O react-router-dom.js https://unpkg.com/react-router-dom/umd/react-router-dom.js
wget -O history.js https://unpkg.com/history/umd/history.js
wget -O subdir/events.js https://unpkg.com/events/events.js

wget -O kontraktor-common.js https://unpkg.com/kontraktor-common/kontraktor-common.js
wget -O kontraktor-client.js https://unpkg.com/kontraktor-client/kontraktor-client.js
