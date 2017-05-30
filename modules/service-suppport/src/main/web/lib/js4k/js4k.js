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

// version 3.33
// JavaScript to Kontraktor bridge
// matches kontraktor 3 json-no-ref encoded remoting
var jskIsNode = false;
if ( typeof module !== 'undefined' && module.exports ) {
  if ( typeof window === 'undefined')
    window = {}; // node
  jskIsNode = true;
  XMLHttpRequest = require("./xmlhttpdummy.js");
}

window.jsk = window.jsk || (function () {

  var futureMap = {}; // future id => promise
  var callMap = {}; // future id => argumentlist to support offline caching
  var currentSocket = { socket: null }; // use indirection to refer to a socket in order to ease reconnects
  var sbIdCount = 1;
  var batch = [];
  var batchCB = [];

  function jsk(){
  }

  var _jsk = new jsk();

  _jsk.futureMap = futureMap; // debug read access
  _jsk.resurrectionCallback = null; // called upon connection resurrection
  _jsk.maxLongpollFailures = 5;
  _jsk.longpollFailureCallback = null; // called after maxLongPollFailures
  _jsk.handleLongpollFailureCallback = function(){
    if (typeof(this.longpollFailureCallback) == 'function'){
      this.longpollFailureCallback.apply();
    }
  };

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
    if ( ! list ) { // if type is ommited default to map
      list = type;
      type = 'map';
    }
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
    delete obj._key; // only quick fix - see: #859
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

  // offline cache callback (http only, not websockets), methods: put( [methodname,args], result ) and get( [methodname,args] ) and getCached([methodname,args])
  _jsk.callCache = null;

  /**
   * Connects to a json-published Actor Server with given url using either websockets, Http-Long-Poll or plain http.
   * Note for "plain http" no push functionality via callbacks can be supported, however pure request response 'ask' and 'tell'
   * works.
   *
   * @param wsurl - e.g. "ws://localhost:8080/ws" or "http://localhost:8080/api"
   * @param connectionMode - 'WS' | 'HTLP' | ['HTTP' is discontinued]
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
    } else if ( connectionMode && connectionMode.uname && connectionMode.token ) { // token
      socket = new _jsk.KontraktorPollSocket(wsurl, true,connectionMode.token, connectionMode.uname );
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
    var cb = null;
    if ( args && args.lengt > 0 && args[args.length-1].typ === 'cbw' ) {
      cb = args[args.length-1];
      args[args.length-1] = null;
    }
    return _jsk.buildJObject( "call", { futureKey: callbackId, queue: 0, method: methodName, receiverKey: receiverKey, serializedArgs: JSON.stringify(_jsk.buildJArray("array",args)), cb: cb } );
  };

  _jsk.KontrActor.prototype.buildCallback = function( callbackId ) {
    return { "typ" : "cbw", "obj" : [ callbackId ] };
  };

  _jsk.KontrActor.prototype.mapCBObjects = function(methodName,argList) {
    for (var i = 0; i < argList.length; i++) {
      if ( typeof argList[i] === 'function' ) { // autogenerate Callback object with given function
        argList[i] = new _jsk.Callback(argList[i]);
      }
      if (argList[i] instanceof _jsk.Callback) {
        var callbackId = sbIdCount++;
        futureMap[callbackId] = argList[i];
        callMap[callbackId] = [methodName,argList];
        argList[i] = this.buildCallback(callbackId);
      }
    }
  };

  _jsk.KontrActor.prototype.sendBatched = function(msg,optCB) {
    batch.push(msg);
    batchCB.push(optCB);
    var socket = this.socketHolder.socket;
    var othis = this;
    socket.triggerNextSend(function () {
      //console.log("send batched \n"+JSON.stringify(batch,null,2));
      var data = JSON.stringify(othis.buildCallList(batch, socket.lpSeqNo));
      var prev = batchCB;
      batch = [];
      batchCB = [];
      socket.send(data).then( function( r,e) {
        if (e) {
          currentSocket.socket.termOpenCBs(prev);
        }
      });
    });
  };

  /**
   * call an actor method returning a promise.
   *
   * "public IPromise myMethod( arg0, arg1, .. );"
   *
   */
  _jsk.NO_RESULT = "NO_RESULT";
  _jsk.KontrActor.prototype.ask = function( methodName, args ) {
    if ( _jsk.callCache && _jsk.callCache.getCached ) {
      var res = _jsk.callCache.getCached( methodName,arguments );
      if ( res !== _jsk.NO_RESULT ) {
        var cbr = new _jsk.Promise();
        setTimeout( function() {
          cbr.complete(res[0],res[1]);
        }, 0 );
        return cbr;
      }
    }
    if ( this.socketHolder.socket === null )
      throw "not connected";
    var argList = [];
    for ( var i = 1; i < arguments.length; i++ )
      argList.push(arguments[i]);
    this.mapCBObjects(methodName,argList);
    var futID = sbIdCount++;
    var cb = new _jsk.Promise();
    futureMap[futID] = cb;
    callMap[futID] = [methodName,argList];
    var msg = this.buildCall( futID, this.id, methodName, argList );
    this.sendBatched.call(this,msg,futID);
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
    if ( _jsk.callCache ) { // also tell cache in order to enable proper cleaning
      _jsk.callCache.put([methodName],null);
    }
    var argList = [];
    for ( var i = 1; i < arguments.length; i++ )
      argList.push(arguments[i]);
    this.mapCBObjects(methodName,argList);
    var msg = this.buildCall( 0, this.id, methodName, argList );
    this.sendBatched.call(this,msg);
    return this;
  };

  function processSocketResponse(lastSeenSequence, decodedResponse, automaticTransformResults, messageListener, listenerThis) {
    var respLen = decodedResponse.seq[0] - 1; // last one is sequence.
    var sequence = decodedResponse.seq[decodedResponse.seq.length-1];
    //console.log("GOT SEQUENCE:"+sequence);
    if ( sequence <= lastSeenSequence ) {
      console.log("old data received:"+sequence+" last:"+lastSeenSequence);
      // return lastSeenSequence;
    }
    //console.log("resplen:"+respLen);
    for (var i = 0; i < respLen; i++) {
      var resp = decodedResponse.seq[i + 1];
      if (!resp.obj.method && resp.obj.receiverKey) { // => callback
        var cb = futureMap[resp.obj.receiverKey];
        if (!cb) {
          console.error("unhandled callback " + JSON.stringify(resp, null, 2));
        } else {
          var methodAndArgs = callMap[resp.obj.receiverKey];
          if ( resp.obj.serializedArgs ) {
            resp.obj.args = JSON.parse(resp.obj.serializedArgs);
            resp.obj.serializedArgs = null;
          }
          if (cb instanceof _jsk.Promise || (cb instanceof _jsk.Callback && resp.obj.args.seq[2] !== 'CNT')) {
            delete futureMap[resp.obj.receiverKey];
            delete callMap[resp.obj.receiverKey];
          }
          if (automaticTransformResults) {
            var transFun = function (obj) {
              if (obj != null && obj instanceof Array && obj.length == 2 && typeof obj[1] === 'string' && obj[1].indexOf("_ActorProxy") > 0) {
                return new _jsk.KontrActor(obj[0], obj[1]); // automatically create remote actor wrapper
              }
              return null;
            };
            var res = _jsk.transform(resp.obj.args.seq[1], true, transFun);
            var err = _jsk.transform(resp.obj.args.seq[2], transFun);
            if ( _jsk.callCache ) {
              _jsk.callCache.put(methodAndArgs,[res,err]);
            }
            cb.complete(res, err); // promise.complete(result, error)
          } else {
            var res = resp.obj.args.seq[1];
            var err = resp.obj.args.seq[2];
            if ( _jsk.callCache ) {
              _jsk.callCache.put(methodAndArgs,[res,err]);
            }
            cb.complete(res, err); // promise.complete(result, error)
          }
        }
      } else {
        messageListener.apply(listenerThis, [resp]);
      }
    }
    return sequence;
  }

  /**
   * Websocket wrapper class. Only difference: methods are used instead of properties for onmessage, onerror, ...
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
      var p = new _jsk.Promise();
      p.complete("",null); // dummy impl
      return p;
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


  _jsk.KontraktorPollSocket = function( url, doLongPoll, token, uname ) {
    var self = this;

    currentSocket.socket = self;

    self.doLongPoll = doLongPoll ? doLongPoll : true;
    self.url = url;
    self.uname = uname;
    self.token = token;
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
    self.pollErrorsInRow = 0;

    function fireOpen() {
      self.onopenHandler.apply(self, [{event: "opened", session: self.sessionId}]);
    }

    // throw an error to all open callbacks and close them or reply with response of cache delegate
    self.termOpenCBs = function(optCBs,error) {
      if ( ! error )
        error = "connection error";
      var keys = optCBs;
      if (optCBs) {
      } else {
        keys = Object.keys(futureMap);
      }
      for (var i = 0; i < keys.length; i++) {
        var cb = futureMap[keys[i]];
        if (cb) {
          var methodAndArgs = callMap[keys[i]];
          var res = null;
          if (_jsk.callCache && (res = _jsk.callCache.get(methodAndArgs))) {
            try {
              cb.complete(res[0], res[1]); // promise.complete(result, error)
            } catch (ex) {
              console.error(ex);
            }
          } else {
            try {
              cb.complete(null, error ); // promise.complete(result, error)
            } catch (ex) {
              console.error(ex);
            }
          }
        }
        delete futureMap[keys[i]];
        delete callMap[keys[i]];
      }
    };

    function fireError() {
      self.termOpenCBs();
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
        var cblen = Object.keys(futureMap).length;
        console.log("futureMap SIZE ON LP *** ", cblen );
        if ( cblen === 0 ) {
          // in case no pending callback or promise is present => skip LP
          setTimeout(self.longPoll,2000);
          return;
        }
        var reqData = '{"styp":"array","seq":[1,'+self.lpSeqNo+']}';
        var request = new XMLHttpRequest();
        request.onreadystatechange = function () {
          // console.log("RESP!!!",JSON.stringify(request));
          if ( request.readyState !== XMLHttpRequest.DONE ) {
            return;
          }
          self.longPollUnderway--;
          if ( request.status !== 200 ) {
            self.lastError = request.statusText;
            console.log("response error:"+request.status);
            //fireError(); dont't give up on failed long poll
            self.pollErrorsInRow++;
            if (self.pollErrorsInRow >= _jsk.maxLongpollFailures){
              _jsk.handleLongpollFailureCallback();
            }
            setTimeout(self.longPoll,3000);
            return;
          }
          self.pollErrorsInRow = 0;
          try {
            // console.log(request);
            var resp = request.responseText; //JSON.parse(request.responseText);
            if ( resp && resp.trim().length > 0 ) {
              var respObject = JSON.parse(resp);
              var sequence = respObject.seq[respObject.seq.length-1];
              self.handleResurrection(sequence);
              self.lpSeqNo = processSocketResponse(self.lpSeqNo, respObject, true, self.onmessageHandler, self);
              setTimeout(self.longPoll,0); // progress, immediately next request
            } else {
              console.log("resp is empty");
              setTimeout(self.longPoll,1000);
            }
          } catch (err) {
            console.log(err);
            setTimeout(self.longPoll,3000); // error, slow down
          }
        };
        if ( self.longPollUnderway == 0 ) {
          request.open("POST", self.url+"/"+self.sessionId, true);
          request.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
          if ( self.token )
            request.setRequestHeader("JWT", self.token );
          self.longPollUnderway++;
          try {
            request.send(reqData); // this is actually auth data currently unused. keep stuff websocket alike for now
          } catch ( ex ) {
            console.log("req error ",ex);
          }
        }
      }
    };

    // set error handler
    self.onerror = function (eventListener) {
      self.onerrorHandler = eventListener;
    };

    // set message handler
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

    self.handleResurrection = function(sequence) {
      if (sequence === 1 && self.lpSeqNo >= 1) {
        self.lpSeqNo = 0;
        console.log("session resurrection, reset sequence ");
        //self.termOpenCBs(); wrong here as resurrection is detected AFTER a valid new callback has been registered
        if (_jsk.resurrectionCallback) {
          _jsk.resurrectionCallback.call(_jsk);
        }
      }
    };

    self.send = function( data ) {
      var res = new _jsk.Promise();
      self.batchUnderway = true;
      var request = new XMLHttpRequest();

      function processRawRepsonse(resp) {
        if (resp && resp.trim().length > 0) {
          try {
            var respObject = JSON.parse(resp);
            //console.log("req:",data);
            //console.log("resp:",resp);
            var sequence = respObject.seq[respObject.seq.length - 1];
            self.handleResurrection(sequence);
            if ( respObject.seq && respObject.seq.length == 3 && typeof respObject.seq[1] === 'string') { // error
              self.termOpenCBs(null,respObject.seq[1]);
              return;
            }
            self.lpSeqNo = processSocketResponse(self.lpSeqNo, respObject, true, self.onmessageHandler, self);
          } catch (ex) {
            console.error("exception in callback ", ex);
            res.complete(null, ex);
          }
          try {
            res.complete("", null);
          } catch (ex1) {
            console.error("exception in promise callback ", ex1);
          }
        } else {
          console.log("resp is empty");
          res.complete("", null);
        }
      }

      request.onreadystatechange = function () {
        if ( request.readyState !== XMLHttpRequest.DONE ) {
          return;
        }
        self.batchUnderway = false;
        if ( self.sendFun ) {
          var tmp = self.sendFun;
          self.sendFun = null;
          tmp.apply(self,[]);
        }
        if ( request.status !== 200 ) {
          self.lastError = request.statusText;
          res.complete(null,request.status);
          return;
        }
        processRawRepsonse(request.responseText);
      };

      request.open("POST", self.url+"/"+self.sessionId, true);
      request.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
      if ( self.token )
        request.setRequestHeader("JWT", self.token );
      if ( self.uname )
        request.setRequestHeader("ID", self.token );

      try {
        request.send(data);
      } catch (ex) {
        console.error(ex);
        res.complete(null,ex);
      }
      return res;
    };

    self.onclose = function( eventListener ) {
      self.oncloseHandler = eventListener;
    };

    // connect and obtain sessionId
    var request = new XMLHttpRequest();
    request.onreadystatechange = function () {
      if ( request.readyState !== XMLHttpRequest.DONE ) {
        return;
      }
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
    if ( self.token )
      request.setRequestHeader("JWT", self.token );
    if ( self.uname )
      request.setRequestHeader("ID", self.uname );
    request.send("null"); // this is actually auth data currently unused. keep stuff websocket alike for now
    return this;
  };

  return _jsk;
}());

if ( jskIsNode ) {
  module.exports = window.jsk;
}
