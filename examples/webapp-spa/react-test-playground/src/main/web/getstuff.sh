#!/usr/bin/env bash

# gets latest version from unpkg.com. adapt urls for a fixed version
# take care, we need browser compatible builds (umd)

# blueprint requires react-with-addons
wget -O lib/react-with-addons.js https://unpkg.com/react/dist/react-with-addons.js
wget -O lib/react-dom.js https://unpkg.com/react-dom/dist/react-dom.js
#wget -O lib/react-router-dom.js https://unpkg.com/react-router-dom/umd/react-router-dom.js
#wget -O lib/history.js https://unpkg.com/history/umd/history.js
#wget -O lib/subdir/events.js https://unpkg.com/events/events.js

# blueprintjs
wget -O lib/blueprintjs/classnames.js                    https://unpkg.com/classnames@^2.2
wget -O lib/blueprintjs/dom4.js                          https://unpkg.com/dom4@^1.8
wget -O lib/blueprintjs/tether.js                        https://unpkg.com/tether@^1.4
wget -O lib/blueprintjs/blueprintjs.js      https://unpkg.com/@blueprintjs/core

# bootstrap
wget -O lib/react-bootstrap.js https://unpkg.com/react-bootstrap/dist/react-bootstrap.js

# kontraktor
wget -O lib/kontraktor-common.js https://unpkg.com/kontraktor-common/kontraktor-common.js
wget -O lib/kontraktor-client.js https://unpkg.com/kontraktor-client/kontraktor-client.js

