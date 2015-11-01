/*
 * Copyright 2014 Ruediger Moeller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// version 3.12.0
// JavaScript to Kontraktor bridge
// matches kontraktor 3.0 json-no-ref encoded remoting
// as I am kind of a JS beginner, hints are welcome :)
if ( typeof window === 'undefined')
  window = {}; // node
window.jsk = window.jsk || (function () {

  var futureMap = {}; // future id => promise
  var currentSocket = { socket: null }; // use indirection to refer to a socket in order to ease reconnects
  var sbIdCount = 1;
  var batch = [];

  function jsk(){
  }

  var _jsk = new jsk();

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // fst-Json Helpers

  /**
   * create wrapper object to make given list a valid fst-json Java array (non-primitive)
   *
   * @param type - 'array' for object array, else type of java array
   * @param list - list of properly structured (for java) objects
   */
  jsk.prototype.buildJArray = function( type, list ) {
    list.splice( 0, 0, list.length ); // insert number of elements at 0
    return { styp: type, seq: list };
  };
  jsk.prototype.jarray = jsk.prototype.buildJArray;

  // shorthand for object array
  jsk.prototype.oa = function( jsArr ) {
    return _jsk.jarray("array",jsArr);
  };

  /**
   * transforms a java hashmap to a JS map, assumes keys are strings
   * @param jmap
   */
  jsk.prototype.toJSMap = function(jmap) {
    var res = {};
    for ( var i = 0; i < jmap.length; i+=2 ) {
      res[jmap[i]] = jmap[i+1];
    }
    return res;
  };

  /**
   *
   * build java list style collection
   * Does not work for java Map's
   *
   * @param type - 'list' for ArrayList, 'set' for HashSet, class name for subclassed
   * @param list - list of properly structured (for java) objects
   */
  jsk.prototype.buildJColl = function( type, list ) {
    list.splice( 0, 0, list.length ); // insert number of elements at 0
    return { typ: type, obj: list };
  };
  jsk.prototype.jcoll = jsk.prototype.buildJColl;

  /**
   * builds a java hashmap from array like '[ key, val, key, val ]'
   *
   * if list is an object, build a hashmap from the properties of that object
   *
   * @param type - "map" or class name if subclassed map is used
   * @param list - of key, val, key1, val1
   */
  jsk.prototype.buildJMap = function( type, list ) {
    if( Object.prototype.toString.call( list ) === '[object Array]' ) {
        list.splice( 0, 0, list.length/2 ); // insert number of elements at 0
        return { typ: type, obj: list };
    } else {
      var res = { typ: type, obj: [] };
      var count = 0;
      for (var property in list) {
        if (list.hasOwnProperty(property)) {
            count++;
            res.obj.push(property);
            res.obj.push(list[property]);
        }
      }
      res.obj.splice( 0, 0, res.obj.length/2 ); // insert number of elements at 0
      return res;
    }
  };
  jsk.prototype.jmap = jsk.prototype.buildJMap;

  /**
   * create wrapper object to make given list a valid fst-json Java Object for sending
   */
  jsk.prototype.buildJObject = function( type, obj ) {
    return { typ: type, obj: obj }
  };
  jsk.prototype.jobj = jsk.prototype.buildJObject;

  /**
   * makes a fst json serialized object more js-friendly
   * @param obj
   * @param preserveTypeAsAttribute - create a _typ property on each object denoting original java type
   * @param optionalTransformer - called as a map function for each object. if returns != null => replace given object in tree with result
   * @returns {*}
   */
  jsk.prototype.transform = function(obj, preserveTypeAsAttribute, optionalTransformer) {
    if (optionalTransformer) {
      var trans = optionalTransformer.apply(null, [obj]);
      if (trans)
        return trans;
    }
    if (!obj)
      return obj;
    if (obj["styp"] && obj["seq"]) {
      var arr = this.transform(obj["seq"], preserveTypeAsAttribute, optionalTransformer);
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
      var res = this.transform(obj["obj"], preserveTypeAsAttribute, optionalTransformer);
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
          obj[property] = this.transform(obj[property], preserveTypeAsAttribute, optionalTransformer);
        } else if (obj[property].constructor == Array) {
          for (var i = 0; i < obj[property].length; i++) {
            obj[property][i] = this.transform(obj[property][i], preserveTypeAsAttribute, optionalTransformer);
          }
        }
      }
    }
    return obj;
  };

  /////////////////////////////////////////////////////////////////////////////////////////////
  // actor remoting helper

  // map from typeName to a mapping 'function( object ) returns obj'.
  // e.g. jsk.postObjectDecodingMap["com.myclass.Foo"] = function( obj ) { obj.attr = ..; return obj; }
  _jsk.postObjectDecodingMap = {};

  /**
   * Connects to a json-published Actor Server with given url using either websockets, Http-Long-Poll or plain http.
   * Note for "plain http" no push functionality via callbacks can be supported, however pure request response 'ask' and 'tell'
   * works.
   *
   * @param wsurl - e.g. "ws://localhost:8080/ws" or "http://localhost:8080/api"
   * @param connectionMode - 'WS' | 'HTLP' | 'HTTP'
   * @param optErrorcallback
   * @returns {jsk.Promise}
   */
  _jsk.socket = null;
  _jsk.remoteApp = null;
  _jsk.connect = function(wsurl, connectionMode, optErrorcallback) {
    var res = new _jsk.Promise();
    if ( _jsk.socket != null ) {
      res.complete(_jsk.remoteApp,null);
      return res;
    }
    var socket = null;
    if ( 'WS' === connectionMode ) {
      socket = new _jsk.KontraktorSocket(wsurl);
    } else if ( "HTLP" === connectionMode || "HTTP" === connectionMode ) {
      socket = new _jsk.KontraktorPollSocket(wsurl, "HTLP" === connectionMode );
    } else {
      console.log("unknown connectionMode, default to HTLP");
      socket = new _jsk.KontraktorPollSocket(wsurl, true );
    }
    var myHttpApp = new _jsk.KontrActor(1,"RemoteApp");
    socket.onmessage( function(message) {
      if ( optErrorcallback ) {
        optErrorcallback.apply(null,[message]);
      } else if (typeof message === MessageEvent ) {
        console.error("unexpected message:"+message.data);
      } else {
        console.error("unexpected message");
        console.log(JSON.stringify(message, null, 2));
      }
    });
    socket.onerror( function(err) {
      if ( ! res.isCompleted() )
        res.complete(null,err);
      if ( optErrorcallback )
        optErrorcallback.apply(null,[err]);
      else
        console.log(err);
    });
    socket.onclose( function() {
      _jsk.socket = null;
      if ( ! res.isCompleted() )
        res.complete(null,"closed");
      if ( optErrorcallback )
        optErrorcallback.apply(null,["closed"]);
      else
        console.log("close");
    });
    socket.onopen( function (event) {
      _jsk.socket = socket;
      _jsk.remoteApp = myHttpApp;
      res.complete(myHttpApp,null);
    });
    return res;
  };

  /**
   * Minimalistic Promise class. FIXME: add timeout feature
   *
   * @param optional initialResult
   * @constructor
   */
  _jsk.Promise = function(initialResult) {
    this.res = initialResult ? [initialResult,null] : null;
    this.cb = null;
    this.nextPromise = null;
  };
  _jsk.Promise.prototype.isCompleted = function() { return this.res; };
  _jsk.Promise.prototype._notify = function() {
    var res = this.cb.apply(null,this.res);
    this.cb = null;
    if ( res instanceof _jsk.Promise ) {
      res.then(this.nextPromise);
    } else {
      this.nextPromise.complete(this.res[0],this.res[1]);
    }
  };

  _jsk.Promise.prototype.then = function(cb) {
    if ( this.cb )
      throw "double callback registration on promise";
    this.cb = cb;
    this.nextPromise = new _jsk.Promise();
    if ( this.res ) {
      this._notify();
    }
    return this.nextPromise;
  };

  _jsk.Promise.prototype.complete = function(r,e) {
    if ( this.res )
      throw "double completion on promise";
    this.res = [r,e];
    if ( this.cb ) {
      this._notify();
    }
  };

  /**
   * Wrapper for callbacks from remote actor's
   *
   * @param resultsCallback
   * @constructor
   */
  _jsk.Callback = function(resultsCallback) {
    if ( ! resultsCallback ) {
      throw "must register callback before sending";
    }
    this.complete = resultsCallback;
  };


  /**
   * A wrapper for a server side Actor
   */
  _jsk.KontrActor = function( id, optionalType ) {
    this.id = id;
    this.type = optionalType ? optionalType : "untyped Actor";
    this.socketHolder = currentSocket;
  };

  /**
   * create a sequenced batch of remote calls
   */
  _jsk.KontrActor.prototype.buildCallList = function( list, seqNo ) {
    var l = list.slice();
    l.push(seqNo);
    return _jsk.buildJArray("array",l);
  };

  /**
   *
   * @param callbackId - callback id in case method has a promise as a result
   * @param receiverKey - target actor id
   * @param args - [] of properly formatted fst-json JavaObjects
   * @returns {{typ, obj}|*}
   */
  _jsk.KontrActor.prototype.buildCall = function( callbackId, receiverKey, methodName, args ) {
    return _jsk.buildJObject( "call", { futureKey: callbackId, queue: 0, method: methodName, receiverKey: receiverKey, args: _jsk.buildJArray("array",args) } );
  };

  _jsk.KontrActor.prototype.buildCallback = function( callbackId ) {
    return { "typ" : "cbw", "obj" : [ callbackId ] };
  };

  _jsk.KontrActor.prototype.mapCBObjects = function(argList) {
    for (var i = 0; i < argList.length; i++) {
      if ( typeof argList[i] === 'function' ) { // autogenerate Callback object with given function
        argList[i] = new _jsk.Callback(argList[i]);
      }
      if (argList[i] instanceof _jsk.Callback) {
        var callbackId = sbIdCount++;
        futureMap[callbackId] = argList[i];
        argList[i] = this.buildCallback(callbackId);
      }
    }
  };

  _jsk.KontrActor.prototype.sendBatched = function(msg) {
    batch.push(msg);
    var socket = this.socketHolder.socket;
    var othis = this;
    socket.triggerNextSend(function () {
      //console.log("send batched \n"+JSON.stringify(batch,null,2));
      var data = JSON.stringify(othis.buildCallList(batch, socket.lpSeqNo));
      batch = [];
      socket.send(data);
    });
  };

    /**
   * call an actor method returning a promise.
   *
   * "public IPromise myMethod( arg0, arg1, .. );"
   *
   */
  _jsk.KontrActor.prototype.ask = function( methodName, args ) {
    if ( this.socketHolder.socket === null )
      throw "not connected";
    var argList = [];
    for ( var i = 1; i < arguments.length; i++ )
      argList.push(arguments[i]);
    this.mapCBObjects(argList);
    var futID = sbIdCount++;
    var cb = new _jsk.Promise();
    futureMap[futID] = cb;
    var msg = this.buildCall( futID, this.id, methodName, argList );
    this.sendBatched.call(this,msg);
    return cb;
  };

  /**
   * call a simple asynchronous method returning nothing
   *
   * "public void myMethod( arg0, arg1, .. );"
   */
  _jsk.KontrActor.prototype.tell = function( methodName, args ) {
    if ( this.socketHolder.socket === null )
      throw "not connected";
    var argList = [];
    for ( var i = 1; i < arguments.length; i++ )
      argList.push(arguments[i]);
    this.mapCBObjects(argList);
    var msg = this.buildCall( 0, this.id, methodName, argList );
    this.sendBatched.call(this,msg);
    return this;
  };

  function processSocketResponse(lastSeenSequence, decodedResponse, automaticTransformResults, messageListener, listenerThis) {
    var respLen = decodedResponse.seq[0] - 1; // last one is sequence. FIXME: should do sequence check here
    var sequence = decodedResponse.seq[decodedResponse.seq.length-1];
    //console.log("GOT SEQUENCE:"+sequence);
    if ( sequence <= lastSeenSequence ) {
      console.log("old data received:"+sequence+" last:"+lastSeenSequence);
      return lastSeenSequence;
    }
    console.log("resplen:"+respLen);
    for (var i = 0; i < respLen; i++) {
      var resp = decodedResponse.seq[i + 1];
      if (!resp.obj.method && resp.obj.receiverKey) { // => callback
        var cb = futureMap[resp.obj.receiverKey];
        if (!cb) {
          console.error("unhandled callback " + JSON.stringify(resp, null, 2));
        } else {
          if (cb instanceof _jsk.Promise || (cb instanceof _jsk.Callback && resp.obj.args.seq[2] !== 'CNT'))
            delete futureMap[resp.obj.receiverKey];
          if (automaticTransformResults) {
            var transFun = function (obj) {
              if (obj != null && obj instanceof Array && obj.length == 2 && typeof obj[1] === 'string' && obj[1].indexOf("_ActorProxy") > 0) {
                return new _jsk.KontrActor(obj[0], obj[1]); // automatically create remote actor wrapper
              }
              return null;
            };
            cb.complete(_jsk.transform(resp.obj.args.seq[1], true, transFun), _jsk.transform(resp.obj.args.seq[2], transFun)); // promise.complete(result, error)
          } else
            cb.complete(resp.obj.args.seq[1], resp.obj.args.seq[2]); // promise.complete(result, error)
        }
      } else {
        messageListener.apply(listenerThis, [resp]);
      }
    }
    return sequence;
  }

  /**
   * Websocket wrapper class. Only difference methods are used instead of properties for onmessage, onerror, ...
   *
   * onmessage parses messages received. If a promise response is received, the promise is invoked. If onmessage
   * receives unrecognized messages, these are passed through
   *
   * @param url
   * @param protocols
   * @constructor
   */
  _jsk.KontraktorSocket = function( url, protocols ) {
    var self = this;

    currentSocket.socket = self;

    var incomingMessages = [];
    var inParse = false;
    self.lpSeqNo = 0; // dummy for now

    self.automaticTransformResults = true;

    if ( protocols )
      self.socket = new WebSocket(url,protocols);
    else
      self.socket = new WebSocket(url);

    self.close = function( code, reaseon ) {
      self.socket.close(code,reaseon);
    };

    /**
     * calls back if ready to send. can be used for batching
     * @param fun
     */
    self.triggerNextSend = function( fun ) {
      fun.apply(this,[]);
    };

    self.send = function( data ) {
      self.socket.send(data);
    };

    self.onclose = function( eventListener ) {
      self.socket.onclose = eventListener;
    };

    self.onmessage = function (eventListener) {
      self.socket.onmessage = function (message) {
        if (typeof message.data == 'string') {
          try {
            var response = JSON.parse(message.data);
            processSocketResponse(-1,response, self.automaticTransformResults, eventListener, self);
          } catch (err) {
            console.error("unhandled decoding error:" + err);
            if (self.socket.onerror)
              self.socket.onerror.apply(self, [err]);
          }
//          eventListener.apply(self, [message]);
        } else {
          incomingMessages.push(message.data);
          // in order to parse binary messages, an async file reader must be used.
          // therefore its necessary to ensure proper ordering of parsed messages.
          // approch taken is to parse the next message only if the previous one has been
          // parsed and processed.
          var parse = function () {
            var fr = new FileReader();
            fr.onabort = function (error) {
              if (self.socket.onerror)
                self.socket.onerror.apply(self, [error]);
              else {
                console.log("unhandled transmission error: " + error);
              }
              if (incomingMessages.length > 0)
                parse.apply();
              else
                inParse = false;
            };
            fr.onerror = fr.onabort;
            fr.onloadend = function (event) {
              try {
                var blob = event.target.result;
                var response = JSON.parse(blob);
                processSocketResponse(-1,response, self.automaticTransformResults, eventListener, self);
              } catch (err) {
                console.error("unhandled decoding error:" + err);
                if (self.socket.onerror)
                  self.socket.onerror.apply(self, [err]);
              }
              if (incomingMessages.length > 0)
                parse.apply();
              else
                inParse = false;
            };
            fr.readAsText(incomingMessages.shift());

          }; // end parse function
          if (!inParse) {
            inParse = true;
            parse.apply();
          }
        }
      }
    };

    self.onerror = function (eventListener) {
      self.socket.onerror = eventListener;
    };

    self.onopen = function (eventListener) {
      self.socket.onopen = function (ev) {
        setTimeout(function () {
          eventListener.apply(self.socket, [ev])
        }, 100); // FIXME: wait for correct state instead of dumb delay
      };
    };

  }; // KontraktorSocket


  _jsk.KontraktorPollSocket = function( url, doLongPoll ) {
    var self = this;

    currentSocket.socket = self;

    self.doLongPoll = doLongPoll ? doLongPoll : true;
    self.url = url;
    self.sessionId = null;
    self.onopenHandler = null;
    self.onerrorHandler = null;
    self.oncloseHandler = null;
    self.lastError = null;
    self.isConnected = false;
    self.doStop = false;
    self.onmessageHandler = null;
    self.lpSeqNo = 0;
    self.batchUnderway = false;

    function fireOpen() {
      self.onopenHandler.apply(self, [{event: "opened", session: self.sessionId}]);
    }

    function fireError() {
      if (self.onerrorHandler && self.lastError) {
        self.onerrorHandler.apply(self, [{event: "connection failure", status: self.lastError}]);
      }
    }

    self.onopen = function(eventHandler) {
      self.onopenHandler = eventHandler;
      if ( self.sessionId ) {
        fireOpen();
      }
      setTimeout(self.longPoll,0);
    };

    self.longPollUnderway = 0;
    self.longPoll = function() {
      if ( self.doStop || ! self.doLongPoll )
        return;
      if ( ! self.isConnected ) {
        setTimeout(self.longPoll,1000);
      } else {
        var reqData = '{"styp":"array","seq":[1,'+self.lpSeqNo+']}';
        var request = new XMLHttpRequest();
        request.onload = function () {
          self.longPollUnderway--;
          if ( request.status !== 200 ) {
            self.lastError = request.statusText;
            console.log("response error:"+request.status);
            fireError();
            setTimeout(self.longPoll,1000);
            return;
          }
          try {
            var resp = request.responseText; //JSON.parse(request.responseText);
            if ( resp && resp.trim().length > 0 ) {
              var respObject = JSON.parse(resp);
              self.lpSeqNo = processSocketResponse(self.lpSeqNo, respObject, true, self.onmessageHandler, self);
              setTimeout(self.longPoll,0);
            } else {
              console.log("resp is empty");
            }
            setTimeout(self.longPoll,0); // progress, immediately next request
          } catch (err) {
            console.log(err);
            setTimeout(self.longPoll,3000); // error, slow down
          }
        };
        if ( self.longPollUnderway == 0 ) {
          request.open("POST", self.url+"/"+self.sessionId, true);
          request.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
          self.longPollUnderway++;
          request.send(reqData); // this is actually auth data currently unused. keep stuff websocket alike for now
        }
      }
    };

    self.onerror = function (eventListener) {
      self.onerrorHandler = eventListener;
      fireError();
    };

    self.onmessage = function(messageHandler) {
      self.onmessageHandler = messageHandler;
    };

    self.close = function( code, reaseon ) {
      self.doStop = true;
      if ( self.oncloseHandler )
        self.oncloseHandler.apply(self,["closed by application"]);
    };


    self.sendFun = null;

    /**
     * calls back if ready to send. can be used for batching
     * @param fun
     */
    self.triggerNextSend = function( fun ) {
      if ( self.batchUnderway )
        self.sendFun = fun;
      else {
        fun.apply(this,[]);
      }
    };

    self.send = function( data ) {
      self.batchUnderway = true;
      var request = new XMLHttpRequest();
      request.onload = function () {
        self.batchUnderway = false;
        if ( self.sendFun ) {
          var tmp = self.sendFun;
          self.sendFun = null;
          tmp.apply(self,[]);
        }
        if ( request.status !== 200 ) {
          self.lastError = request.statusText;
          fireError(); // fixme: should retry batch
          return;
        }
        var resp = request.responseText; //JSON.parse(request.responseText);
        if ( resp && resp.trim().length > 0 ) {
            var respObject = JSON.parse(resp);
            self.lpSeqNo = processSocketResponse(self.lpSeqNo, respObject, true, self.onmessageHandler, self);
        } else {
          console.log("resp is empty");
        }
      };
      request.open("POST", self.url+"/"+self.sessionId, true);
      request.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
      request.send(data);
    };

    self.onclose = function( eventListener ) {
      self.oncloseHandler = eventListener;
    };

    // connect and obtain sessionId
    var request = new XMLHttpRequest();
    request.onload = function () {
      if ( request.status !== 200 ) {
        self.lastError = request.statusText;
        fireError();
        return;
      }
      self.sessionId = JSON.parse(request.responseText);
      self.isConnected = true;
      console.log("sessionId:"+self.sessionId);
      if ( self.onopenHandler ) {
        fireOpen();
      }
    };

    request.open("POST", self.url, true);
    request.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
    request.send("null"); // this is actually auth data currently unused. keep stuff websocket alike for now
    return this;
  };

  return _jsk;
}());

if ( typeof module !== 'undefined' && module.exports )
  module.exports = window.jsk;