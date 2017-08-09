# WebApp using Html import shim of kontraktor-Http4k

Html imports are an emerging standard currently only implemented in chrome (firefox via flag).
As Html-imports enable a structured dependency management for front-ends without requiring
a babel stack. Kontraktor-http provides a server side shim to improve site performance and overcome compatibility issues:

* in DEV mode, you rely on the browser implementation (so easy to debug). Drawback is you are bound to chrome for
development (or fifox with flag turned on)
* in production mode, kontrakotr-http inlines html-imports, so it runs on any modern browser. 

[more on html imports](https://www.html5rocks.com/en/tutorials/webcomponents/imports/)

**how to run**

* add pom.xml to your IDE
* run "WebApp" with working dir [..]/examples/html-import-sample
* goto localhost:8081 (Chrome required for devmode !)

Toggle devmode in WebApp.main and check network/src in your browser

