## Raw WebApp example

Plain js frontend demonstrating communication with server side actors and webapp setup with Http4k.
This is a low magic approach leaving many choices open and can be used with any front-end framework.

* shows how to dynamically switch between websockets or http long-poll
* how to configure http4k and pathes
* server push
* packaging feature (set DEV to false in AppMain [ be warned: js code changes will not be reflected then, caching ] )
check out the index.html in chrome when in prod mode (5,6kb incl. everything).
* how to handle backlinks e.g. from a confirmation mail or dynamically generate documents, send redirects, whatever

**how to run**

* add as maven project
* run MyHttpAppMain with working dir [..]/kontraktor/examples/http-ws-javascript-es6
* goto localhost:8080

run ./web/getstuff.sh in order to update libs (requires npm)

this is the low level kontraktor webapp interface. checkout react-material-ui for an advanced example 
([...]/kontraktor/examples/webapp-spa/react-ui-lib-examples/react-material-ui)

see kontraktor/attic/http-ws.. for a version using the old js4k.js API [does not use es6-proxies]