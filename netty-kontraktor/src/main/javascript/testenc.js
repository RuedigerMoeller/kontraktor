
var JTestEncoding = function(obj) {
    this.__typeInfo = 'TestEncoding';
    this.receiverKey=obj;
    this._actorProxy = true;


    this.numbers = function(by, sh, ch, integ, f, d) {
        var call = MinBin.obj('call', {
            method: 'numbers',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                MinBin.parseIntOrNan(by),
                MinBin.parseIntOrNan(sh),
                MinBin.parseIntOrNan(ch),
                MinBin.parseIntOrNan(integ),
                MinBin.parseIntOrNan(f),
                MinBin.parseIntOrNan(d)
            ])
        });
        return Kontraktor.send(call,true);
    };
    this.arrays = function(oa, str, by, sh, ch, integ, f, d) {
        var call = MinBin.obj('call', {
            method: 'arrays',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                MinBin.jarray(oa),
                MinBin.jarray(str),
                MinBin.i8(by),
                MinBin.i16(sh),
                MinBin.ui16(ch),
                MinBin.i32(integ),
                MinBin.dbl(f),
                MinBin.dbl(d)
            ])
        });
        return Kontraktor.send(call,true);
    };
    this.lists = function(al, li) {
        var call = MinBin.obj('call', {
            method: 'lists',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                MinBin.jlist(al),
                MinBin.jlist(li)
            ])
        });
        return Kontraktor.send(call,true);
    };
    this.hmap = function(mp) {
        var call = MinBin.obj('call', {
            method: 'hmap',
            receiverKey: this.receiverKey,
            args: MinBin.jarray([
                MinBin.jmap(val)
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


var JMap = function(obj) {
    this.__typeInfo = 'Map';



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


var JLinkedList = function(obj) {
    this.__typeInfo = 'LinkedList';



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


var JArrayList = function(obj) {
    this.__typeInfo = 'ArrayList';

    this.j_size = function() { return MinBin.parseIntOrNan(this.size); };


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



var mbfactory = function(clzname,jsObjOrRefId) {
    switch (clzname) {
        case 'TestEncoding': return new JTestEncoding(jsObjOrRefId);
        case 'Map': return new JMap(jsObjOrRefId);
        case 'LinkedList': return new JLinkedList(jsObjOrRefId);
        case 'ArrayList': return new JArrayList(jsObjOrRefId);
        default: if (!jsObjOrRefId) return { __typeInfo: clzname }; else { jsObjOrRefId.__typeInfo = clzname; return jsObjOrRefId; }
    }
};

MinBin.installFactory(mbfactory);
