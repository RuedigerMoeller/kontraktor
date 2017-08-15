# Kontraktor **without** babel/node (**intrinsic** react/jsx support) 

This example uses kontraktor-web, see [here](https://github.com/RuedigerMoeller/kontraktor/tree/trunk/examples/react-intrinsic-minimal) for a minimalistic kontraktor-http based example. 

Uses a java-implemented jsx transpiler (+ light es6 import+object spread shim)

* if mode is set to production, transpilation will performed once on start, not with each request 
(see ./run/etc/app.kson)
* react server side rendering is not supported (and probably not worth the hassle)

This examples uses proxies and does not transpile to es5. 
Works in newer chrome, firefox, edge, safari browsers.
Proxy usage can be turned off (KClient).

shims (only applied to .jsx):
* import
* object spread operator (can be turned off in JSXParser static)

check [documentation](https://github.com/RuedigerMoeller/kontraktor/wiki/Kontraktor-4-React-JSX) for details
