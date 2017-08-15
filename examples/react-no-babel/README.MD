# Kontraktor **without** babel/node (**intrinsic** react/jsx support) 

Uses a java-implemented jsx transpiler (+ light es6 import+object spread shim)

* if mode is set to production, transpilation will performed once on start, not with each request 
(see ./run/etc/app.kson)
* react server side rendering is not supported (and probably not worth the hassle)

This examples uses proxies and does not transpile to es5. 
Works in newer chrome, firefox, edge, safari browsers.
Proxy usage can be turned off (KClient).

* build or add to ide using mvn pom.xml
* run ReactAppMain with working Dir [..]/react-no-babel
* goto localhost:8080
* login with 'qweqwe' 'qweqwe' (or register)
