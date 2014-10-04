// requires minbin.js

var Kontraktor = new function() {

    var self = this;
    this.ws = null; // a Websocket object
    this.socketConnected = false;

    var cbmap = {};
    var cbid = 1;

    this.call = function( msg ) {
        if ( ! self.socketConnected ) {
            throw "socket is closed";
        }
        var args = msg.args;
        for ( var i = 0; i < args.length; i++ ) {
            if ( typeof args[i] == 'function' ) {
                cbmap[cbid] = args[i];
                args[i] = MinBin.obj("cbw", MinBin.jarray([cbid]) );
                cbid++;
            }
        }
        this.ws.send(MinBin.encode(msg));
    };

    this.sendBinary = function( binmsg ) {
        if ( ! self.socketConnected ) {
            throw "socket is closed";
        }
        this.ws.send(binmsg);
    };

    this.connect = function(host,port,websocketDir,onOpen) {
        this.ws = new WebSocket("ws://".concat(host).concat(":").concat(port).concat("/").concat(websocketDir));

        this.ws.onopen = function () {
            self.socketConnected = true;
            if ( onOpen != null )
                onOpen.apply();
        };

        this.ws.onerror = function () {
            console.log("error");
            this.socketConnected = false;
            this.close();
        };

        this.ws.onclose = function () {
            console.log("closed");
            self.socketConnected = false;
        };

        this.ws.onmessage = function (message) {
            var fr = new FileReader();
            if (typeof message.data == 'string') {

            } else {
                // parse binary MinBin message
                fr.onloadend = function (event) {
                    var msg = MinBin.decode(event.target.result);
                    var strMsg = MinBin.prettyPrint(msg);
                    // handle message

                };
                // error handling is missing
                fr.readAsArrayBuffer(message.data);
            }
        };
    };

};
