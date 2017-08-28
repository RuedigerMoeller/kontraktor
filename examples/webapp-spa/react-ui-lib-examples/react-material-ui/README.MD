# react-material-ui with kontraktor intrinsic jnpm, jsx transpilation and bundling (no node/npm/babel required)

**This does not run under Windows currently**, working on that ..

Full example featuring

* jsx transpilation, automatic npm download (without having nodejs/npm installed)
* Session handling
* Session timeout handling
* Session resurrection. (SPA client was away/offline and connects back)
* Dynamic connection type (http adaptive long poll or websockets) without code change

**how to run:**

* add project to your ide (from pom)
* run ReactMaterialUITestApp with workingdir [...]/examples/webapp-spa/react-ui-lib-examples/react-material-ui
* goto localhost:8080 in your browser
* first request triggers download of required npm packages so give it like 50 seconds (wathc console)
* set DEVMODE to false to get a (dynamically) bundled build
