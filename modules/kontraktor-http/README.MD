## Kontraktor Http

### More Connectors for kontraktor distributed Actors

* Http Long Poll and WebSocket based remoting connectors for kontraktor distributed actors (client and server)
* a javascript client and server lib (kontraktor-client, kontraktor-server, deprecated:js4k.js) for
interop with nodejs services and browser

### Single Page Application Support

* can switch transparently at runtime between http-long-poll / websockets
* user session handling (timeout/resurrection for temporary disconnected clients) 
* uses embedded webserver (Red Hats Undertow)
* java implemented JSX transpiler 
* java implemented npm-clone
* java implemented bundling/minification (npm, es6 imports, html-imports)
* in the making: servlet support

You can use modern js-frontends like react-bootstrap, material-ui, semantic-ui-react without the need
to even install nodejs/npm.

**Recommended Example**: kontraktor/examples/webapp-spa/react-ui-lib-examples/react-material-ui (full feature set)

**Documentation**: https://github.com/RuedigerMoeller/kontraktor/wiki/Kontraktor-4.


