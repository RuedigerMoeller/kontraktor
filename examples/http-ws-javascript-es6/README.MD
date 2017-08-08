
Plain js frontend demonstrating communication with server side actors and Http4k.

* shows how to dynamically switch between websockets or http long-poll
* how to configure http4k and pathes
* server push
* packaging feature (set DEV to false in AppMain [be warned: code changes will not be reflected then] )
check out the index.html in chrome when in prod mode.
* how to handle backlinks e.g. from a confirmation mail or generate documents

run ./web/getstuff.sh in order to update libs (requires npm)

this is the low level kontraktor webapp interface, checkout kontraktor-web for a more frameworky approach.

see kontraktor/attic/http-ws.. for a version using the old js4k.js API [does not use es6-proxies]