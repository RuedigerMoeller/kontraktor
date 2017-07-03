const WebSocket = require('ws');

///////////////////////////////// temporary //////////////////////////////////////

class KPromise {
  /**
   * Minimalistic Promise class. FIXME: add timeout feature
   *
   * @param optional initialResult
   * @constructor
   */
  constructor(initialResult) {
    this.res = initialResult ? [initialResult,null] : null;
    this.cb = null;
    this.nextPromise = null;
  };

  isCompleted() { return this.res; };

  _notify() {
    var res = this.cb.apply(null,this.res);
    this.cb = null;
    if ( res instanceof KPromise ) {
      res.then(this.nextPromise);
    } else {
      this.nextPromise.complete(this.res[0],this.res[1]);
    }
  };

  then(cb) {
    if ( this.cb )
      throw "double callback registration on promise";
    this.cb = cb;
    this.nextPromise = new KPromise();
    if ( this.res ) {
      this._notify();
    }
    return this.nextPromise;
  };

  complete(r,e) {
    if ( this.res )
      throw "double completion on promise";
    this.res = [r,e];
    if ( this.cb ) {
      this._notify();
    }
  };

}

  /**
   * makes a fst json serialized object more js-friendly
   * @param obj
   * @param preserveTypeAsAttribute - create a _typ property on each object denoting original java type
   * @param optionalTransformer - called as a map function for each object. if returns != null => replace given object in tree with result
   * @returns {*}
   */
  function transformJavaJson(obj, preserveTypeAsAttribute, optionalTransformer) {
    if (optionalTransformer) {
      var trans = optionalTransformer.apply(null, [obj]);
      if (trans)
        return trans;
    }
    if (!obj)
      return obj;
    if (obj["styp"] && obj["seq"]) {
      var arr = transformJavaJson(obj["seq"], preserveTypeAsAttribute, optionalTransformer);
      if (arr) {
        arr.shift();
        if (preserveTypeAsAttribute)
          arr["_typ"] = obj["styp"];
      }
      return arr;
    }
    if (obj["typ"] && obj["obj"]) {
      if ('list' === obj['typ'] || 'map' === obj['typ'] ) {
        // remove leading element length from arraylist, map
        obj["obj"].shift();
      }
      var res = transformJavaJson(obj["obj"], preserveTypeAsAttribute, optionalTransformer);
      if (preserveTypeAsAttribute)
        res["_typ"] = obj["typ"];
      if ( typeof _jsk.postObjectDecodingMap[res["_typ"]] !== 'undefined' ) { // aplly mapping if present
        res = _jsk.postObjectDecodingMap[res["_typ"]].apply(null,[res]);
      }
      return res;
    }
    for (var property in obj) {
      if (obj.hasOwnProperty(property) && obj[property] != null) {
        if (obj[property].constructor == Object) {
          obj[property] = transformJavaJson(obj[property], preserveTypeAsAttribute, optionalTransformer);
        } else if (obj[property].constructor == Array) {
          for (var i = 0; i < obj[property].length; i++) {
            obj[property][i] = transformJavaJson(obj[property][i], preserveTypeAsAttribute, optionalTransformer);
          }
        }
      }
    }
    return obj;
  }


///////////////////////////////// temporary //////////////////////////////////////


const wss = new WebSocket.Server({ port: 3999 });

class TestActor {

  hello( str ) {
    console.log("hello called ",str);
    return new KPromise(str+" from node");
  }

}
var service = new TestActor();


wss.on('connection', function connection(ws) {

  // console.log("connection ",ws);

  ws.on('connect', function incoming(message) {
    const msg = JSON.parse(message);
    console.log('connect:', msg);
  });

  ws.on('message', function incoming(message) {
    const msg = JSON.parse(message);
    console.log('message:', msg);
    if ( msg.styp === 'array' ) {
      const msgarr = msg.seq;
      if ( msgarr.length > 1) {
        for ( var i = 1; i < msgarr.length-1; i++ ) {
          if ( msgarr[i].typ === 'call' ) {
            let call = msgarr[i].obj;
            if ( call.serializedArgs != null ) {
              call.args = transformJavaJson(JSON.parse(call.serializedArgs),true);
              call.serializedArgs = null;
            }
            console.log("remote call:", call);
            service[call.method].apply(service,call.args);
          }
        }
      }
    }
  });

});
