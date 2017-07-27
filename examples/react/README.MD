Kontraktor + React ( **in the making, 3.34 is not on maven yet** )

Uses a nodejs service to perform browserify and babel.

* if mode is set to production, transpilation will performed once on start, not with each request
* in dev full babel + browserify is performed (+sourcemaps are generated). **do not judge performance and file size from 'dev mode'** 
* in production mode source maps are omitted and libs are inlined and minified (once upon first request)
* react server side rendering is not supported (and probably not worth the hassle)

### How to run

* install nodejs 6+, npm
* run getstuff.sh inside (workingdir matters)  [..]/react/src/main/web/client 
* add java projects (also those from 'modules') from pom.xml in intellij or eclipse as kontraqktor 3.34 is not on maven yet
* run ReactApp with working dir in project root ([..]/examples/react)

