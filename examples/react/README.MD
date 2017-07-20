Kontraktor + React (in the making, 3.34 is not on maven yet)

Uses a nodejs service to perform browserify and babel.

* if mode is set to production, transpilation will performed once on start, not with each request
* in dev full babel + browserify is performed

### How to run

* install nodejs 6+, npm
* run getstuff.sh inside (workingdir matters)  [..]/react/src/main/web/client 
* add java projects (also those from 'modules') from pom.xml in intellij or eclipse
* run ReactApp with working dir in project root ([..]/examples/react)
* Note: there are good plugins for IntelliJ for react .jsx file editing, set javascript version in IDE to es6 