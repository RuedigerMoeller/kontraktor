Kontraktor + React ( **in the making, 3.34 is not on maven yet** )

Uses a nodejs service to perform browserify and babel.

* if mode is set to production, transpilation will performed once on start, not with each request
* in dev full babel + browserify is performed (+sourcemaps are generated). **do not judge performance and file size from 'dev mode'** 
* in production mode source maps are omitted and libs are inlined and minified (once upon first request).
see ./run/etc/app.kson .
* react server side rendering is not supported (and probably not worth the hassle)

### How to run

* install nodejs 6+, npm
* run getstuff.sh inside (workingdir matters)  [..]/react/src/main/web/client 
* run ReactApp with working dir in project root ([..]/examples/react)
* note: this was tested under Linux, it might be required to run babelserver manually if kontraktor fails to start it:
`~/IdeaProjects/kontraktor/examples/react/src/main/web$ node client/node_modules/babelserver/babelserver.js`
* build or add to ide using mvn pom.xml
* run ReactAppMain with working Dir [..]/react-no-babel
* goto localhost:8080
* login with 'qweqwe' 'qweqwe' (or register)
