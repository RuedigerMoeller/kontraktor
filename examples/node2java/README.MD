
#  Connect a java actor from node.js

example on how to connect and talk from node.js to a java actor via websockets. Note its required to do some java friendly json formatting in order to have java understand JS object structures. (**Note:** all of below holds true when connecting from a browser to a kontraktor based webservice).

### Typesafe way

Data received from java is transformed in a way its comparably easy to consume, however when sending data to typed actor methods, some formatting has to be done (see example code).

### Easy route

The easy route is to use "Unknown" arguments (see example) to pass arbitrary json structures, however its still required to tag arrays like

Object array (results in java.lang.Object[])
```javascript
jsk.oa( [ { x: 3, me: "not" }, 13 ] );
```

int array, double array (result in primitive arrays on java side)
```javascript
[1,2,3,4] // int
["double", 2.3, 3.5 ] // double needs tag
```

