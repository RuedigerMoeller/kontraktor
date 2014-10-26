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
    this.restPrefix = 'rest';

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

    // in case no generation has been done, invoke methods on remote actors.
    // targetId = actor id (1=facade actor)
    // withFuture must be true in case called method has a future result
    this.call = function( targetId, methodName, withFuture, args ) {
        var call = MinBin.obj("call", {
            "method": methodName,
            "receiverKey": targetId,
            "args": MinBin.jarray(args)
        });
        return self.send(call, withFuture);
    };

    // private, don't use
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
            console.log("put future for method "+msg.method+" to "+self.cbid);
            self.cbid++;
            this.ws.send(MinBin.encode(msg));
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

    this.restGET = function(url) {
        var promise = new KPromise();
        var xhr = new XMLHttpRequest();
        xhr.onload = function (e) {
            if (xhr.readyState === 4) {
                if (xhr.status === 200) {
                    try {
                        var r = JSON.parse(xhr.responseText);
                        console.log(xhr.responseText);
                        promise.receive(r.args[0], r.args[1]);
                    } catch (err) {
                        promise.receive(xhr.responseText, err);
                    }
                } else {
                    promise.receive(null, xhr.statusText);
                }
            }
        };
        xhr.onerror = function (e) {
            promise.receive(null, xhr.statusText);
        };
        xhr.open( "GET", self.restPrefix+'/'+url, true );
        xhr.send( null );
        return promise;
    };

    this.connectHome = function(onOpen) {
        self.connect(window.location.hostname, window.location.port, 'websocket', onOpen);
    };

    this.connect = function(host,port,websocketDir,onOpen) {
        var url = "ws://".concat(host).concat(":").concat(port).concat("/").concat(websocketDir);
        console.log('connecting '+url);
        this.ws = new WebSocket(url);

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

/////////////////////////////////// requires KO (tested with 3.2.0) /////////////////////////////////////

var krServer; // { facade: [facadeactor], session: [sessionActor], meta: [reallive datamodel] }

if ( typeof ko !== 'undefined') {
    krServer = {
        facade:  ko.observable(null),
        session: ko.observable(null),
        meta:    ko.observable(null),
        loggedIn: ko.observable(false)
    };

    ko.components.register( 'kr-login', {
            viewModel:
            function (params) {
                var self = this;
                this.user = ko.observable('');
                this.pwd = ko.observable('');
                this.resultMsg = ko.observable('');
                this.facadeClass = params.facade;   // required
                this.loggedIn = krServer.loggedIn;

                this.loginDone = function () {
                    self.resultMsg('');
                    krServer.loggedIn(true);
                };
                // expect $authenticate(), FIXME: define webfacade iface
                this.login = function () {
                    Kontraktor.restGET('$authenticate/'+self.user()+'/'+self.pwd()).then( function(r,e) {
                        if ( e ) {
                            self.resultMsg(e);
                        } else {
                            Kontraktor.connectHome( function() {
                                var facadeClz = window[self.facadeClass];
                                krServer.facade(new facadeClz(1));
                                krServer.facade().$getSession(r).then( function(r,e) {
                                    if ( e ) {
                                        self.resultMsg(e);
                                    } else if (r==null) {
                                        self.resultMsg('unable to create session');
                                    } else {
                                        krServer.session(r);
                                        if (typeof RealLive !== 'undefined') { // load model
                                            krServer.session().$getRLMeta().then( function(model,err) {
                                                if ( err ) {
                                                    self.resultMsg(err);
                                                } else {
                                                    krServer.meta(model);
                                                    self.loginDone();
                                                }
                                            });
                                        } else
                                            self.loginDone();
                                    }
                                });
                            })
                        }
                    });
                }.bind(this);
            },
            template:
                '<span data-bind="visible: !loggedIn()">\
                    <input placeholder="user" size="6" type="text" data-bind="value: user">\
                    <input placeholder="password" size="4" type="password" data-bind="value: pwd">\
                    <button data-bind="click: login">Log In</button>\
                </span>\
                <span data-bind="visible: loggedIn()">Welcome <b data-bind="text: user"></b></span>\
                <b><span data-bind="text: resultMsg" style="color: darkred;">\
                </span></b>'
        }
    );
}