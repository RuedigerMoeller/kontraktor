// requires minbin.js

var Kontraktor = new function() {

    this.ws = null; // a Websocket object
    this.socketConnected = false;

    this.connect = function(host,port,websocketDir) {
        this.ws = new WebSocket("ws://".concat(host).concat(":").concat(port).concat("/").concat(websocketDir));
        this.ws.onopen = function () {

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
            var _thisWS = this;
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