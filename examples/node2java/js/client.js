//module.paths.push('/home/ruedi/projects/kontraktor/modules/kontraktor-http/src/main/javascript/js4k');
module.paths.push('C:\\work\\GitHub\\kontraktor\\modules\\kontraktor-http\\src\\main\\javascript\\js4k');

var jsk = require("js4k.js");

// replace some browser API
WebSocket = require('websocket').w3cwebsocket;
FileReader = require('filereader');

jsk.connect("ws://localhost:8080/s4n","WS").then( function( res, err ) {

    if ( res ) {
        console.log("connected");
        res.tell("hello", "from node");
        res.ask("concat", 13, 17, ",").then( function(r,e) {
            console.log("result:'"+r+"'");
        });
        res.ask("typeTest", 77, 13.2, 14.1414, ["a","b","c"], [1,2,3], ["double",10.0,20.0,30.0]).then( function(r,e) {
            console.log(JSON.stringify(r));
        });
        res.ask("mapOut").then( function(r,e) {
            console.log("mapOut:"+JSON.stringify(r));
            res.ask("mapIn", jsk.buildJMap( "map", r )).then(function(r,e) {
                console.log("mapIn:"+JSON.stringify(r));
            });
        });
    } else {
        console.log("res "+res+" err "+err);
    }

});
//var client = new WebSocket('ws://localhost:8080/', 'echo-protocol');
//
//client.onerror = function() {
//    console.log('Connection Error');
//};
//
//client.onopen = function() {
//    console.log('WebSocket Client Connected');
//
//    function sendNumber() {
//        if (client.readyState === client.OPEN) {
//            var number = Math.round(Math.random() * 0xFFFFFF);
//            client.send(number.toString());
//            setTimeout(sendNumber, 1000);
//        }
//    }
//    sendNumber();
//};
//
//client.onclose = function() {
//    console.log('echo-protocol Client Closed');
//};
//
//client.onmessage = function(e) {
//    if (typeof e.data === 'string') {
//        console.log("Received: '" + e.data + "'");
//    }
//};