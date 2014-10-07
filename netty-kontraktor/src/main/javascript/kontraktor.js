// requires minbin.js
var KPromise = function(initialResult) {
    this.res = initialResult ? [initialResult,null] : null;
    this.isPromise = false;
    this.cb = null;

    this.then = function(cb) {
        if ( this.res )
            cb.apply(null,this.res);
        else
            this.cb = cb;
    };
    this.receive = function(r,e) {
        if ( this.cb ) {
            this.cb.apply(null,[r,e]);
            this.cb = null;
        } else
            this.res = [r,e];
    };

};

var kendsWith = function endsWith(str, suffix) {
    return str.indexOf(suffix, str.length - suffix.length) !== -1;
};

var Kontraktor = new function() {

    var self = this;
    this.ws = null; // a Websocket object
    this.socketConnected = false;

    this.cbmap = {};
    this.cbid = 13;

    this.registerLocalActor = function(actor) {
        if ( actor._actorProxy ) // use generated stubs !
        {
            this.cbmap[this.cbid] = actor;
            actor.receiverKey = this.cbid;
            this.cbid++;
        } else
            throw "not a valid actor clazz";
    };

    this.registerForeignActorRef = function(actor) {
        // a foreign actorref is handed out to an external process, just map, don't assign id
        // an actor ref recieverKey contains the id in a foreign process. If this ref is handed out
        // a second id is required to dispatch calls incoming on this ref.
        if ( actor._actorProxy ) // use generated stubs !
        {
            if ( ! actor._foreignRefKey ) { // already registered
                this.cbmap[this.cbid] = actor;
                actor._foreignRefKey = this.cbid;
                this.cbid++;
            }
        } else
            throw "not a valid actor clazz";
    };

    this.send = function( msg, withFuture ) {
        if ( ! self.socketConnected ) {
            throw "socket is closed";
        }
        var args = msg.args;
        if ( args ) {
            for (var i = 0; i < args.length; i++) {
                if (typeof args[i] == 'function') {
                    self.cbmap[self.cbid] = args[i];
                    args[i].isPromise = false;
                    args[i] = MinBin.obj("cbw", MinBin.jarray([self.cbid]));
                    self.cbid++;
                }
                if ( args[i] && args[i]._actorProxy ) { // foreign ref handed out to other process
                    Kontraktor.registerForeignActorRef(args[i]);
                }
            }
        } else {
            msg.args = MinBin.jarray([]);
        }
        if ( withFuture ) {
            var res = new KPromise();
            res.isPromise = true;
            self.cbmap[self.cbid] = res;
            msg.futureKey = self.cbid;
            this.ws.send(MinBin.encode(msg));
            self.cbid++;
            return res;
        } else {
            this.ws.send(MinBin.encode(msg));
            return null;
        }
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
            if ( onOpen != null ) {
                onOpen.apply(null,[true]);
            }
            var ping = function() {
                if ( self.socketConnected ) {
                    self.ws.send("KTR_PING");
                    setTimeout(ping,7000);
                }
            };
            ping.apply();
        };

        this.ws.onerror = function () {
            console.log("connection error");
            this.socketConnected = false;
            this.close();
            if ( onOpen != null )
                onOpen.apply("socket error");
        };

        this.ws.onclose = function () {
            console.log("connection closed");
            self.socketConnected = false;
            if ( onOpen != null )
                onOpen.apply("socket closed");
        };

        this.ws.onmessage = function (message) {
            var fr = new FileReader();
            if (typeof message.data == 'string') {

            } else {
                // parse binary MinBin message
                fr.onloadend = function (event) {
                    var msg = MinBin.decode(event.target.result);
                    var strMsg = MinBin.prettyPrint(msg);
                    console.log("callback:\n "+strMsg);
                    // handle message
                    if ( msg.queue || msg.queue == 0 ) {
                        if ( msg.queue == 1 ) { // callback
                            var cbfunc = self.cbmap[msg.receiverKey];
                            if ( cbfunc ) {
                                if ( cbfunc.isPromise ) {
                                    delete self.cbmap[msg.receiverKey];
                                    cbfunc.receive(msg.args[0],msg.args[1]);
                                } else {
                                    if ("CNT" != msg.args[1]) {
                                        delete self.cbmap[msg.receiverKey];
                                    }
                                    cbfunc.apply(null, msg.args);
                                }
                            } else
                                console.error("no function found for callback");
                        } else if ( msg.queue == 0 ) {
                            var actor = self.cbmap[msg.receiverKey];
                            var futKey = msg.futureKey;
                            if (!actor)
                                throw "unknown actor with id "+msg.receiverKey;
                            var future = actor[msg.method].apply(actor,msg.args);
                            if ( future ) {
                                future.then( function(r,e) {
                                    var call = MinBin.obj("call", {
                                        receiverKey: futKey,
                                        queue: 1,
                                        "args" : MinBin.jarray([ r, e ])
                                    });
                                    Kontraktor.send(call);
                                });
                            }
                        }
                    } else
                        console.error("unrecognized callback message"+msg);
                };
                // error handling is missing
                fr.readAsArrayBuffer(message.data);
            }
        };
    };

};
