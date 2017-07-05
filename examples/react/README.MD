Kontraktor + React (in the making)

Uses a nodejs service to perform browserify and babel.

* if mode is set to production, transpilation will performed once on start, not with each request
* in dev full babel + browserify is performed

### How to run

* install nodejs 6+, npm
* run getstuff.sh inside (workingdir matters)  [..]/react/src/main/web/client and [..]/react/src/main/nodejs 
  (as the babel service and final app requires different modules)
* goto main/nodejs directory. run 'nodejs babelserver.js' (this is the babelify daemon used by the java backend)
* import java project from pom.xml in intellij or eclipse
* run ReactApp with working dir in project root ([..]/examples/react)
* Note: there are good plugins for IntelliJ for react .jsx file editing, set javascript version in IDE to es5 