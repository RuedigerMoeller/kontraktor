
var JWSTestActor = function(obj) {
    this.__typeInfo = 'WSTestActor';
    this.receiverKey=obj;
    this._actorProxy = true;


    this.$voidCall = function(message) {
        var call = MinBin.obj('call', {
            method: '$voidCall',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                message
            ])
        });
        return Kontraktor.send(call);
    };
    this.$futureCall = function(result) {
        var call = MinBin.obj('call', {
            method: '$futureCall',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                result
            ])
        });
        return Kontraktor.send(call,true);
    };
    this.$createSession = function() {
        var call = MinBin.obj('call', {
            method: '$createSession',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
            ])
        });
        return Kontraktor.send(call,true);
    };
    this.$callbackTest = function(msg, cb) {
        var call = MinBin.obj('call', {
            method: '$callbackTest',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                msg,
                MinBin.obj('Callback',cb)
            ])
        });
        return Kontraktor.send(call);
    };
    this.$close = function() {
        var call = MinBin.obj('call', {
            method: '$close',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
            ])
        });
        return Kontraktor.send(call);
    };
    this.$stop = function() {
        var call = MinBin.obj('call', {
            method: '$stop',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
            ])
        });
        return Kontraktor.send(call);
    };
    this.$sync = function() {
        var call = MinBin.obj('call', {
            method: '$sync',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
            ])
        });
        return Kontraktor.send(call,true);
    };

};


var JJSActorInterface = function(obj) {
    this.__typeInfo = 'websockets.WSTestActor$JSActorInterface';
    this.receiverKey=obj;
    this._actorProxy = true;


    this.$futureCall = function(s) { /**/ };
    this.$voidCallClient = function(s) { /**/ };
    this.$close = function() { /**/ };
    this.$stop = function() { /**/ };
    this.$sync = function() { /**/ };

};


var JSampleUser = function(obj) {
    this.__typeInfo = 'SampleUser';

    this.j_age = function() { return MinBin.parseIntOrNan(this.age); };
    this.j_credits = function() { return MinBin.parseIntOrNan(this.credits); };
    this.j_roles = function() { return MinBin.jlist(this.roles); };
    this.j_name = function() { return this.name; };


    this.fromObj = function(obj) {
        for ( var key in obj ) {
            var setter = 'j_'.concat(key);
            if ( this.hasOwnProperty(setter) ) {
                this[key] = obj[key];
            }
        }
        return this;
    };
    if ( obj != null ) {
        this.fromObj(obj);
    }

};


var JClientSession = function(obj) {
    this.__typeInfo = 'ClientSession';
    this.receiverKey=obj;
    this._actorProxy = true;


    this.$init = function(parent) {
        var call = MinBin.obj('call', {
            method: '$init',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                MinBin.obj('WSTestActor',parent)
            ])
        });
        return Kontraktor.send(call);
    };
    this.$setUser = function(user) {
        var call = MinBin.obj('call', {
            method: '$setUser',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                MinBin.obj('SampleUser',user)
            ])
        });
        return Kontraktor.send(call);
    };
    this.$getUser = function() {
        var call = MinBin.obj('call', {
            method: '$getUser',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
            ])
        });
        return Kontraktor.send(call,true);
    };
    this.$registerClientActor = function(actor) {
        var call = MinBin.obj('call', {
            method: '$registerClientActor',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                MinBin.obj('JSActorInterface',actor)
            ])
        });
        return Kontraktor.send(call);
    };
    this.$clientSpecific = function(clientId, whatNot) {
        var call = MinBin.obj('call', {
            method: '$clientSpecific',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                clientId,
                whatNot
            ])
        });
        return Kontraktor.send(call,true);
    };
    this.$close = function() {
        var call = MinBin.obj('call', {
            method: '$close',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
            ])
        });
        return Kontraktor.send(call);
    };
    this.$stop = function() {
        var call = MinBin.obj('call', {
            method: '$stop',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
            ])
        });
        return Kontraktor.send(call);
    };
    this.$sync = function() {
        var call = MinBin.obj('call', {
            method: '$sync',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
            ])
        });
        return Kontraktor.send(call,true);
    };

};



var mbfactory = function(clzname,jsObjOrRefId) {
    switch (clzname) {
        case 'WSTestActor': return new JWSTestActor(jsObjOrRefId);
        case 'JSActorInterface': return new JJSActorInterface(jsObjOrRefId);
        case 'SampleUser': return new JSampleUser(jsObjOrRefId);
        case 'ClientSession': return new JClientSession(jsObjOrRefId);
        default: if (!jsObjOrRefId) return { __typeInfo: clzname }; else { jsObjOrRefId.__typeInfo = clzname; return jsObjOrRefId; }
    }
};

MinBin.installFactory(mbfactory);
