// requires minbin.js

var K = new function() {

    var self = this;

    self.Promise = function(initialResult) {
        this.res = initialResult ? [initialResult,null] : null;
        this.isPromise = false;
        this.cb = null;
        this.isCompleted = false;

        this.then = function(cb) {
            if ( this.res )
                cb.apply(null,this.res);
            else
                this.cb = cb;
        };
        this.complete = function(r,e) {
            if ( this.isCompleted )
                throw "double completion on promise";
            this.res = [r,e];
            this.isCompleted = true;
            if ( this.cb ) {
                this.cb.apply(null,this.res);
                this.cb = null;
            }
        };

    };

    self.endsWith = function endsWith(str, suffix) {
        return str.indexOf(suffix, str.length - suffix.length) !== -1;
    };


    self.ws = null; // a Websocket object
    self.socketConnected = false;
    self.restPrefix = 'http';
    self.sessionId = '';

    self.cbmap = {};
    self.cbid = 13;

    self.registerLocalActor = function(actor) {
        if ( actor._actorProxy ) // use generated stubs !
        {
            this.cbmap[this.cbid] = actor;
            actor.receiverKey = this.cbid;
            this.cbid++;
        } else
            throw "not a valid actor clazz";
    };

    self.registerForeignActorRef = function(actor) {
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

    // in case no generation has been done, invoke methods on remote actors.
    // targetId = actor id (1=facade actor)
    // withFuture must be true in case called method has a future result
    self.call = function( targetId, methodName, withFuture, args ) {
        var call = MinBin.obj("call", {
            "method": methodName,
            "receiverKey": targetId,
            "args": MinBin.jarray(args)
        });
        return self.send(call, withFuture);
    };

    self.unregisterCB = function(cb) {
        if ( cb == null ) {
            console.log("remove cb, cb is null!");
            return;
        }
        console.log("remove cb "+cb.__cbid);
        console.log("callback size "+Object.keys(self.cbmap).length);
        delete self.cbmap[cb.__cbid];
    };

    // private, don't use
    self.send = function( msg, withFuture ) {
        if ( ! self.socketConnected ) {
            throw "socket is closed";
        }
        var args = msg.args;
        if ( args ) {
            for (var i = 0; i < args.length; i++) {
                if (typeof args[i] == 'function') {
                    self.cbmap[self.cbid] = args[i];
                    args[i].__cbid = self.cbid;
                    args[i].isPromise = false;
                    args[i] = MinBin.jarray([self.cbid]);
                    args[i].__typeInfo = "cbw";
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
            var res = new K.Promise();
            res.isPromise = true;
            self.cbmap[self.cbid] = res;
            msg.futureKey = self.cbid;
            console.log("put future for method "+msg.method+" to "+self.cbid);
            self.cbid++;
            this.ws.send(MinBin.encode(msg));
            return res;
        } else {
            this.ws.send(MinBin.encode(msg));
            return null;
        }
    };

    self.sendBinary = function( binmsg ) {
        if ( ! self.socketConnected ) {
            throw "socket is closed";
        }
        this.ws.send(binmsg);
    };

    self.authenticateAndConnectSession = function( user, pwd, serverProxy, socketCallback /* 'error', 'close' */ ) {
        var result = new K.Promise();
        window.setTimeout( function() {
            if ( ! result.isCompleted ) {
                result.complete( null, "login timed out")
            }
        }, 5000 );
        self.restGET("api/$authenticate/"+user+"/"+pwd)
            .then( function(res, err) {
                if ( res ) {
                    // a reconnect with existing session id would start here ..
                    K.connectHome( function(msg) {
                        if ( msg === true ) {
                            serverProxy.receiverKey = 1;
                            serverProxy.$getSession(res).then( function(session,e) {
                                if ( result.isCompleted )
                                    return;
                                self.sessionId = res;
                                if ( session ) {
                                    console.log( "got session "+session);
                                    var ping = function() {
                                        session.$heartBeat();
                                        if ( self.socketConnected ) {
                                            setTimeout(ping,15000);
                                        }
                                    };
                                    ping.apply();
                                    result.complete(session,null);
                                } else {
                                    result.complete( null, e );
                                }
                            });
                        } else {
                            if ( socketCallback )
                                socketCallback.apply( null, [msg] );
                        }
                    })
                } else {
                    if ( ! result.isCompleted ) {
                        result.complete( null, err );
                    }
                }
            });
        return result;
    };

    self.restGET = function(url) {
        var promise = new K.Promise();
        var xhr = new XMLHttpRequest();
        xhr.onload = function (e) {
            if (xhr.readyState === 4) {
                if (xhr.status === 200) {
                    try {
                        var r = JSON.parse(xhr.responseText);
                        r = r[0];
                        console.log(xhr.responseText);
                        promise.complete(r.args[0], r.args[1]);
                    } catch (err) {
                        promise.complete(xhr.responseText, err);
                    }
                } else {
                    promise.complete(null, xhr.statusText);
                }
            }
        };
        xhr.onerror = function (e) {
            promise.complete(null, xhr.statusText);
        };
        xhr.open( "GET", self.restPrefix+'/'+url, true );
        xhr.send( null );
        return promise;
    };

    self.connectHome = function(socketCallback) {
        self.connect(window.location.hostname, window.location.port, window.location.pathname+'ws', socketCallback);
    };

    self.connect = function(host,port,websocketDir,socketCB) {
        var url = "ws://".concat(host).concat(":").concat(port).concat(websocketDir);
        console.log('connecting '+url);
        this.ws = new WebSocket(url);

        this.ws.onopen = function () {
            self.socketConnected = true;
            if ( socketCB != null ) {
                socketCB.apply(null,[true]);
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
            if ( socketCB != null )
                socketCB.apply(null,["error"]);
        };

        this.ws.onclose = function () {
            console.log("connection closed");
            self.socketConnected = false;
            if ( socketCB != null )
                socketCB.apply(null,["closed"]);
        };

        this.ws.onmessage = function (message) {
            var fr = new FileReader();
            if (typeof message.data == 'string') {
                console.error("unexpected message:"+message.data);
            } else {
                // parse binary MinBin message
                fr.onloadend = function (event) {
                    var msg0 = MinBin.decode(event.target.result);
                    var msgArr;
                    //var strMsg = MinBin.prettyPrint(msg);
                    if ( msg0.__typeInfo === 'array' ) {
                        msgArr = msg0;
                    } else {
                        msgArr = [msg0];
                    }
//                    console.log("callback:\n "+strMsg);
                    // handle message
                    var arrayLength = msgArr.length
                    for ( var ai = 0; ai < arrayLength; ai++ ) {
                        var msg = msgArr[ai];
                        if ( msg.queue || msg.queue == 0 ) {
                            if ( msg.queue == 1 ) { // callback
                                var cbfunc = self.cbmap[msg.receiverKey];
                                if ( cbfunc ) {
                                    if ( cbfunc.isPromise ) {
                                        delete self.cbmap[msg.receiverKey];
                                        cbfunc.complete(msg.args[0],msg.args[1]);
                                    } else {
                                        if ("CNT" != msg.args[1]) {
                                            console.log("finishing callback transmission "+msg.args[0])
                                            delete self.cbmap[msg.receiverKey];
                                        }
                                        //if ( msg.args[0] && msg.args[0].tableId && msg.args[0].tableId == 'Instrument' ) {
                                        //    console.log("instr update: "+msg.args[0]);
                                        //}
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
                    }
                };
                // error handling is missing
                fr.readAsArrayBuffer(message.data);
            }
        };
    };

};
