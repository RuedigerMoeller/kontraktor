
var JWSTestActor = function(obj) {
    this.__typeInfo = 'WSTestActor';
    this.receiverKey=obj;
    this._actorProxy = true;


    this.$createTestEncoding = function() {
        var call = MinBin.obj('call', {
            method: '$createTestEncoding',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
            ])
        });
        return Kontraktor.send(call,true);
    };
    this.$futureCall = function(arg0) {
        var call = MinBin.obj('call', {
            method: '$futureCall',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                arg0
            ])
        });
        return Kontraktor.send(call,true);
    };
    this.$voidCall = function(arg0) {
        var call = MinBin.obj('call', {
            method: '$voidCall',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                arg0
            ])
        });
        return Kontraktor.send(call);
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
    this.$callbackTest = function(arg0, arg1) {
        var call = MinBin.obj('call', {
            method: '$callbackTest',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                arg0,
                MinBin.obj('Callback',arg1)
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
    this.$sync = function() {
        var call = MinBin.obj('call', {
            method: '$sync',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
            ])
        });
        return Kontraktor.send(call,true);
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

};


var JTestEncoding = function(obj) {
    this.__typeInfo = 'TestEncoding';
    this.receiverKey=obj;
    this._actorProxy = true;


    this.hmap = function(arg0) {
        var call = MinBin.obj('call', {
            method: 'hmap',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                MinBin.jmap(arg0)
            ])
        });
        return Kontraktor.send(call,true);
    };
    this.numbers = function(arg0, arg1, arg2, arg3, arg4, arg5) {
        var call = MinBin.obj('call', {
            method: 'numbers',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                MinBin.parseIntOrNan(arg0, 'byte' ),
                MinBin.parseIntOrNan(arg1, 'short' ),
                MinBin.parseIntOrNan(arg2, 'char' ),
                MinBin.parseIntOrNan(arg3, 'int' ),
                arg4,
                arg5
            ])
        });
        return Kontraktor.send(call,true);
    };
    this.arrays = function(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7) {
        var call = MinBin.obj('call', {
            method: 'arrays',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                MinBin.jarray(arg0),
                MinBin.jarray(arg1),
                MinBin.i8(arg2),
                MinBin.i16(arg3),
                MinBin.ui16(arg4),
                MinBin.i32(arg5),
                MinBin.dbl(arg6),
                MinBin.dbl(arg7)
            ])
        });
        return Kontraktor.send(call,true);
    };
    this.lists = function(arg0, arg1) {
        var call = MinBin.obj('call', {
            method: 'lists',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                MinBin.jlist(arg0),
                MinBin.jlist(arg1)
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
    this.$sync = function() {
        var call = MinBin.obj('call', {
            method: '$sync',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
            ])
        });
        return Kontraktor.send(call,true);
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

};


var JJSActorInterface = function(obj) {
    this.__typeInfo = 'websockets.WSTestActor$JSActorInterface';
    this.receiverKey=obj;
    this._actorProxy = true;


    this.$futureCall = function(arg0) { /**/ };
    this.$testFinished = function() { /**/ };
    this.$voidCallClient = function(arg0) { /**/ };
    this.$close = function() { /**/ };
    this.$sync = function() { /**/ };
    this.$stop = function() { /**/ };

};


var JSampleUser = function(obj) {
    this.__typeInfo = 'SampleUser';

    this.j_age = function() { return MinBin.parseIntOrNan(this.age, 'int' ); };
    this.j_credits = function() { return MinBin.parseIntOrNan(this.credits, 'int' ); };
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


    this.$registerClientActor = function(arg0) {
        var call = MinBin.obj('call', {
            method: '$registerClientActor',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                MinBin.obj('JSActorInterface',arg0)
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
    this.$pingRound = function(arg0) {
        var call = MinBin.obj('call', {
            method: '$pingRound',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                MinBin.obj('ClientSession',arg0)
            ])
        });
        return Kontraktor.send(call);
    };
    this.$print = function(arg0) {
        var call = MinBin.obj('call', {
            method: '$print',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                arg0
            ])
        });
        return Kontraktor.send(call,true);
    };
    this.$sendLoop = function(arg0) {
        var call = MinBin.obj('call', {
            method: '$sendLoop',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                MinBin.parseIntOrNan(arg0, 'int' )
            ])
        });
        return Kontraktor.send(call);
    };
    this.$clientSpecific = function(arg0, arg1) {
        var call = MinBin.obj('call', {
            method: '$clientSpecific',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                arg0,
                arg1
            ])
        });
        return Kontraktor.send(call,true);
    };
    this.$setUser = function(arg0) {
        var call = MinBin.obj('call', {
            method: '$setUser',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                MinBin.obj('SampleUser',arg0)
            ])
        });
        return Kontraktor.send(call);
    };
    this.$init = function(arg0) {
        var call = MinBin.obj('call', {
            method: '$init',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                MinBin.obj('WSTestActor',arg0)
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
    this.$sync = function() {
        var call = MinBin.obj('call', {
            method: '$sync',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
            ])
        });
        return Kontraktor.send(call,true);
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

};



var mbfactory = function(clzname,jsObjOrRefId) {
    switch (clzname) {
        case 'WSTestActor': return new JWSTestActor(jsObjOrRefId);
        case 'TestEncoding': return new JTestEncoding(jsObjOrRefId);
        case 'JSActorInterface': return new JJSActorInterface(jsObjOrRefId);
        case 'SampleUser': return new JSampleUser(jsObjOrRefId);
        case 'ClientSession': return new JClientSession(jsObjOrRefId);
        default: if (!jsObjOrRefId) return { __typeInfo: clzname }; else { jsObjOrRefId.__typeInfo = clzname; return jsObjOrRefId; }
    }
};

MinBin.installFactory(mbfactory);
