# Testbed react with kontraktor (without node/babel) + 3rd party libs

semantic-ui-react showcase. 

* basic session timeout handling
* Does not include session resurrection and 
websocket connectivity. See react-material-ui example for example code of those.

**how to run:**
* add project to your ide (from pom)
* run ReactSemanticUITestApp with workingdir [...]/examples/webapp-spa/react-ui-lib-examples/semantic-ui
* goto localhost:8080 in your browser
* first request triggers download of required npm packages so give it like 50 seconds (wathc console)
* set DEVMODE to false to get a (dynamically) bundled build
