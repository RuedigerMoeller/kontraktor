module.paths.push('/home/ruedi/projects/kontraktor/modules/kontraktor-http/src/main/javascript/js4k');
//module.paths.push('C:\\work\\GitHub\\kontraktor\\modules\\kontraktor-http\\src\\main\\javascript\\js4k');

var jsk = require("js4k.js");

// replace some browser API
WebSocket = require('websocket').w3cwebsocket;
FileReader = require('filereader');

jsk.connect("ws://localhost:8080/s4n","WS").then( function( remoteActor, err ) {

    if ( remoteActor ) {
        console.log("connected");
        remoteActor.tell("hello", "from node");
        remoteActor.ask("concat", 13, 17, ",").then( function(r,e) {
            console.log("concat:\n'"+r+"'");
        });
        remoteActor.ask("typeTest", 77, 13.2, 14.1414, ["a","b","c"], [1,2,3], ["double",10.0,20.0,30.0]).then( function(r,e) {
            console.log("typetest:\n"+JSON.stringify(r, null, 2));
        });
        remoteActor.ask("mapOut").then( function(r,e) {
            console.log("mapOut:"+JSON.stringify(r));
            var javamap = jsk.jmap("map", r); // need to construct proper java hashmap
            remoteActor.ask("mapIn", javamap).then(function(r,e) {
                console.log("mapIn returned:\n"+JSON.stringify(r, null, 2));
            });
        });
        remoteActor.ask("pojoOut").then( function( pojo, err ) {
            console.log("pojoOut:\n"+JSON.stringify(pojo, null, 2));
            // construct a valid Pojo2Node java Object and send it
            var javaPojo = jsk.jobj(
                "Pojo2Node", // can shorten class name because registered (see Service4Node main)
                {
                    name: "aname",
                    names: [ "a", "b" ],
                    someInt: 13,
                    someInts: [123,345,567,768678],
                    someDouble: 3.1415,
                    someDoubles: ["double", 1.3, 4.566], // required to tag primitive array type
                    embeddedMap: jsk.jmap("map", [ // construct valid java.HashMap. array contains key,value, key1, value1
                        "key", jsk.jarray( "array", [123,"value", [1,32]] ), // value is objectarray
                        "key1", "Value1",
                        13, "numerickey"
                    ])
                }
              );
            //console.log("printing:"+JSON.stringify(javaPojo, null, 2))
            remoteActor.ask( "pojoIn", javaPojo).then( function (r,e) {
                console.log("pojoIn returned:\n"+JSON.stringify(r, null, 2));
            })
        });

         //easiest way (but type unsafe, slowish) is using unknown to pass arbitrary JS structures
        remoteActor.tell("receiveUnknown", {
            x: 13,
            y: 14.5,
            z: "Hallo",
            arr: [ "double", 1.0, 2.0 ],
            xx: jsk.oa([
                { name: "One", number: 0 },
                { name: "Two", number: 18 }
            ]),
            yy: {
                a: 13, b: "Hello planet"
            }
        });
    } else {
        console.log("remoteActor "+remoteActor+" err "+err);
    }

});
