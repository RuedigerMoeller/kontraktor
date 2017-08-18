#!/usr/bin/env bash

# gets latest version from unpkg.com. adapt urls for a fixed version
# take care, we need browser compatible builds (umd)
wget -O lib/react.js https://unpkg.com/react/dist/react.js
wget -O lib/react-dom.js https://unpkg.com/react-dom/dist/react-dom.js
wget -O lib/react-bootstrap.js https://unpkg.com/react-bootstrap/dist/react-bootstrap.js
#wget -O lib/react-router-dom.js https://unpkg.com/react-router-dom/umd/react-router-dom.js
#wget -O lib/history.js https://unpkg.com/history/umd/history.js
#wget -O lib/subdir/events.js https://unpkg.com/events/events.js

wget -O lib/kontraktor-common.js https://unpkg.com/kontraktor-common/kontraktor-common.js
wget -O lib/kontraktor-client.js https://unpkg.com/kontraktor-client/kontraktor-client.js

