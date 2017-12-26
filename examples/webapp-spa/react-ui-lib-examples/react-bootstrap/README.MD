# Testbed react with kontraktor (without node/babel) + 3rd party libs

react-bootstrap showcase. 

* Does not include session timeout, session resurrection and 
websocket connectivity. See react-material-ui example for examples of this.

**how to run:**

* add project to your ide (from pom)
* run ReactMaterialUITestApp with workingdir [...]/examples/webapp-spa/react-ui-lib-examples/react-bootstrap
* goto localhost:8080 in your browser
* first request triggers download of required npm packages so give it like 50 seconds (wathc console)
* set DEVMODE to false to get a (dynamically) bundled build

**Note:** hot module reloading is not enabled (see wiki, semantic ui example on how to do that)
