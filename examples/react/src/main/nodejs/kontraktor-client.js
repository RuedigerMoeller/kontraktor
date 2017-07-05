// ES6 Port of js4k 3.34
// matches kontraktor 3 json-no-ref encoded remoting
const kontraktor = require('./kontraktor-common.js');

const KPromise = kontraktor.KPromise;
const coder = new kontraktor.DecodingHelper();
const NO_RESULT = "NO_RESULT";

var _kontraktor_IsNode = false;

if ( typeof module !== 'undefined' && module.exports ) {
  if ( typeof window === 'undefined')
    window = {}; // node
  _kontraktor_IsNode = true;
  XMLHttpRequest = require("./xmlhttpdummy.js");
}

// a (batching) kontraktor client
class KClient {

  constructor() {
    this.currentSocket = {socket:null};
    this.futureMap = {}; // future id => promise
    this.callMap = {}; // future id => argumentlist to support offline caching
    this.currentSocket = { socket: null }; // use indirection to refer to a socket in order to ease reconnects
    this.sbIdCount = 1;
    this.batch = [];
    this.batchCB = [];
  }

  processSocketResponse(lastSeenSequence, decodedResponse, automaticTransformResults, messageListener) {
    const respLen = decodedResponse.seq[0] - 1; // last one is sequence.
    const sequence = decodedResponse.seq[decodedResponse.seq.length-1];
    //console.log("GOT SEQUENCE:"+sequence);
    if ( sequence <= lastSeenSequence ) {
      console.log("old data received:"+sequence+" last:"+lastSeenSequence);
      // return lastSeenSequence;
    }
    //console.log("resplen:"+respLen);
    for (let i = 0; i < respLen; i++) {
      const resp = decodedResponse.seq[i + 1];
      if (!resp.obj.method && resp.obj.receiverKey) { // => callback
        const cb = this.futureMap[resp.obj.receiverKey];
        if (!cb) {
          console.error("unhandled callback " + JSON.stringify(resp, null, 2));
        } else {
          const methodAndArgs = this.callMap[resp.obj.receiverKey];
          if ( resp.obj.serializedArgs ) {
            resp.obj.args = JSON.parse(resp.obj.serializedArgs);
            resp.obj.serializedArgs = null;
          }
          if (cb instanceof KPromise || (cb instanceof Callback && resp.obj.args.seq[2] !== 'CNT')) {
            delete this.futureMap[resp.obj.receiverKey];
            delete this.callMap[resp.obj.receiverKey];
          }
          if (automaticTransformResults) {
            const transFun = obj => {
              if (obj != null && obj instanceof Array && obj.length == 2 && typeof obj[1] === 'string' && obj[1].indexOf("_ActorProxy") > 0) {
                return new KontrActor(obj[0], obj[1]); // automatically create remote actor wrapper
              }
              return null;
            };
            const res = coder.transform(resp.obj.args.seq[1], true, transFun);
            const err = coder.transform(resp.obj.args.seq[2], transFun);
            if ( this.callCache ) {
              this.callCache.put(methodAndArgs,[res,err]);
            }
            cb.complete(res, err); // promise.complete(result, error)
          } else {
            const res = resp.obj.args.seq[1];
            const err = resp.obj.args.seq[2];
            if ( this.callCache ) {
              this.callCache.put(methodAndArgs,[res,err]);
            }
            cb.complete(res, err); // promise.complete(result, error)
          }
        }
      } else {
        messageListener(resp);
      }
    }
    return sequence;
  }

}

/**
 * represents a remote proxy for a server side actor
 */
class KontrActor {
}

/**
 * Socket wrapper class (can emulate a socket via long poll or wraps an actual websocket).
 *
 * onmessage parses messages received. If a promise response is received, the promise is invoked. If onmessage
 * receives unrecognized messages, these are passed through.
 *
 * @param url
 * @param protocols
 * @constructor
 */
class KontraktorSocket {

  constructor(global, url, protocols) {
    this.global = global;
    global.currentSocket.socket = this;

    this.incomingMessages = [];
    this.inParse = false;
    this.lpSeqNo = 0; // dummy for now
    this.automaticTransformResults = true;

    if ( protocols )
      this.socket = new WebSocket(url,protocols);
    else
      this.socket = new WebSocket(url);
  }

  close( code, reason ) {
    this.socket.close(code,reason);
  }

  /**
   * calls back if ready to send. can be used for batching
   * @param fun
   */
  triggerNextSend( fun ) {
    fun.apply(this,[]);
  };

  send( data ) {
    this.socket.send(data);
    return new KPromise("",null);
  };

  onclose( eventListener ) {
    this.socket.onclose = eventListener;
  };

  onmessage(eventListener) {
    this.socket.onmessage = message => {
      if (typeof message.data == 'string') {
        try {
          const response = JSON.parse(message.data);
          this.global.processSocketResponse(-1,response, this.automaticTransformResults, eventListener, this);
        } catch (err) {
          console.error("unhandled decoding error:" + err);
          if (this.socket.onerror)
            this.socket.onerror.apply(this, [err]);
        }
      } else {
        this.incomingMessages.push(message.data);
        // in order to parse binary messages, an async file reader must be used.
        // therefore its necessary to ensure proper ordering of parsed messages.
        // approach taken is to parse the next message only if the previous one has been
        // parsed and processed.
        const parse = () => {
          const fr = new FileReader();
          fr.onabort = error => {
            if (this.socket.onerror)
              this.socket.onerror.apply(this, [error]);
            else {
              console.log("unhandled transmission error: " + error);
            }
            if (this.incomingMessages.length > 0)
              parse.apply();
            else
              this.inParse = false;
          };
          fr.onerror = fr.onabort;
          fr.onloadend = event => {
            try {
              const blob = event.target.result;
              const response = JSON.parse(blob);
              this.global.processSocketResponse(-1,response, this.automaticTransformResults, eventListener.bind(this));
            } catch (err) {
              console.error("unhandled decoding error:" + err);
              if (this.socket.onerror)
                this.socket.onerror.apply(this, [err]);
            }
            if (this.incomingMessages.length > 0)
              parse.apply();
            else
              this.inParse = false;
          };
          fr.readAsText(this.incomingMessages.shift());
        }; // end parse function
        if (!this.inParse) {
          this.inParse = true;
          parse.apply();
        }
      }
    }
  };

  onerror(eventListener) {
    this.socket.onerror = eventListener;
  };

  onopen(eventListener) {
    this.socket.onopen = ev => {
      setTimeout(() => {
        eventListener.apply(this.socket, [ev])
      }, 100); // FIXME: wait for correct state instead of dumb delay
    };
  };

} // KontraktorSocket

class KontraktorPollSocket{

  constructor( global, url, doLongPoll, token, uname ) {
    global.currentSocket.socket = this;
    this.global = global;
    this.doLongPoll = doLongPoll ? doLongPoll : true;
    this.url = url;
    this.uname = uname;
    this.token = token;
    this.sessionId = null;
    this.onopenHandler = null;
    this.onerrorHandler = null;
    this.oncloseHandler = null;
    this.lastError = null;
    this.isConnected = false;
    this.doStop = false;
    this.onmessageHandler = null;
    this.lpSeqNo = 0;
    this.batchUnderway = false;
    this.pollErrorsInRow = 0;
    this.longPollUnderway = 0;
    this.sendFun = null;
    this.connect();
  }

  fireOpen() {
    this.onopenHandler.apply(self, [{event: "opened", session: this.sessionId}]);
  }

  // throw an error to all open callbacks and close them or reply with response of cache delegate
  termOpenCBs(optCBs,error) {
    if ( ! error )
      error = "connection error";
    let keys = optCBs;
    if (optCBs) {
    } else {
      keys = Object.keys(this.global.futureMap);
    }
    for (let i = 0; i < keys.length; i++) {
      const cb = this.global.futureMap[keys[i]];
      if (cb) {
        const methodAndArgs = this.global.callMap[keys[i]];
        let res = null;
        if (this.global.callCache && (res = this.global.callCache.get(methodAndArgs))) {
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
      delete this.global.futureMap[keys[i]];
      delete this.global.callMap[keys[i]];
    }
  };

  fireError() {
    this.termOpenCBs();
    if (this.onerrorHandler && this.lastError) {
      this.onerrorHandler.apply(self, [{event: "connection failure", status: this.lastError}]);
    }
  }

  onopen(eventHandler) {
    this.onopenHandler = eventHandler;
    if ( this.sessionId ) {
      this.fireOpen();
    }
    setTimeout(this.longPoll,0);
  };

  longPoll() {
    if ( this.doStop || ! this.doLongPoll )
      return;
    if ( ! this.isConnected ) {
      setTimeout(this.longPoll,1000);
    } else {
      const cblen = Object.keys(futureMap).length;
      console.log("futureMap SIZE ON LP *** ", cblen );
      if ( cblen === 0 ) {
        // in case no pending callback or promise is present => skip LP
        setTimeout(this.longPoll,2000);
        return;
      }
      const reqData = '{"styp":"array","seq":[1,'+this.lpSeqNo+']}';
      const request = new XMLHttpRequest();
      request.onreadystatechange = () => {
        // console.log("RESP!!!",JSON.stringify(request));
        if ( request.readyState !== XMLHttpRequest.DONE ) {
          return;
        }
        this.longPollUnderway--;
        if ( request.status !== 200 ) {
          this.lastError = request.statusText;
          console.log("response error:"+request.status);
          //fireError(); dont't give up on failed long poll
          this.pollErrorsInRow++;
          if (this.pollErrorsInRow >= this.global.maxLongpollFailures){
            this.global.handleLongpollFailureCallback();
          }
          setTimeout(this.longPoll,3000);
          return;
        }
        this.pollErrorsInRow = 0;
        try {
          // console.log(request);
          const resp = request.responseText; //JSON.parse(request.responseText);
          if ( resp && resp.trim().length > 0 ) {
            const respObject = JSON.parse(resp);
            const sequence = respObject.seq[respObject.seq.length-1];
            this.handleResurrection(sequence);
            this.lpSeqNo = this.global.processSocketResponse(this.lpSeqNo, respObject, true, this.onmessageHandler.bind(this));
            setTimeout(this.longPoll.bind(this),0); // progress, immediately next request
          } else {
            console.log("resp is empty");
            setTimeout(this.longPoll.bind(this),1000);
          }
        } catch (err) {
          console.log(err);
          setTimeout(this.longPoll.bind(this),3000); // error, slow down
        }
      };
      if ( this.longPollUnderway == 0 ) {
        request.open("POST", this.url+"/"+this.sessionId, true);
        request.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
        if ( this.token )
          request.setRequestHeader("JWT", this.token );
        this.longPollUnderway++;
        try {
          request.send(reqData); // this is actually auth data currently unused. keep stuff websocket alike for now
        } catch ( ex ) {
          console.log("req error ",ex);
        }
      }
    }
  };

  // set error handler
  onerror(eventListener) { this.onerrorHandler = eventListener };
  // set message handler
  onmessage(messageHandler) { this.onmessageHandler = messageHandler; };

  close( code, reaseon ) {
    this.doStop = true;
    if ( this.oncloseHandler )
      this.oncloseHandler.apply(this,["closed by application"]);
  };

  /**
   * calls back if ready to send. can be used for batching
   * @param fun
   */
  triggerNextSend( fun ) {
    if ( this.batchUnderway )
      this.sendFun = fun;
    else {
      fun.apply(this,[]);
    }
  };

  handleResurrection(sequence) {
    if (sequence === 1 && this.lpSeqNo >= 1) {
      this.lpSeqNo = 0;
      console.log("session resurrection, reset sequence ");
      //this.termOpenCBs(); wrong here as resurrection is detected AFTER a valid new callback has been registered
      if (this.global.resurrectionCallback) {
        this.global.resurrectionCallback.call(this.global);
      }
    }
  };

  send( data ) {
    const res = new KPromise();
    this.batchUnderway = true;
    const request = new XMLHttpRequest();

    const processRawResponse = resp => {
      if (resp && resp.trim().length > 0) {
        try {
          const respObject = JSON.parse(resp);
          //console.log("req:",data);
          //console.log("resp:",resp);
          const sequence = respObject.seq[respObject.seq.length - 1];
          this.handleResurrection(sequence);
          if ( respObject.seq && respObject.seq.length == 3 && typeof respObject.seq[1] === 'string') { // error
            this.termOpenCBs(null,respObject.seq[1]);
            return;
          }
          this.lpSeqNo = this.global.processSocketResponse(this.lpSeqNo, respObject, true, this.onmessageHandler.bind(this));
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
    };

    request.onreadystatechange = () => {
      if ( request.readyState !== XMLHttpRequest.DONE ) {
        return;
      }
      this.batchUnderway = false;
      if ( this.sendFun ) {
        const tmp = this.sendFun;
        this.sendFun = null;
        tmp.apply(self,[]);
      }
      if ( request.status !== 200 ) {
        this.lastError = request.statusText;
        res.complete(null,request.status);
        return;
      }
      processRawResponse(request.responseText);
    };

    request.open("POST", this.url+"/"+this.sessionId, true);
    request.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
    if ( this.token )
      request.setRequestHeader("JWT", this.token );
    if ( this.uname )
      request.setRequestHeader("ID", this.token );

    try {
      request.send(data);
    } catch (ex) {
      console.error(ex);
      res.complete(null,ex);
    }
    return res;
  };

  onclose( eventListener ) {
    this.oncloseHandler = eventListener;
  };

  connect() {
    // connect and obtain sessionId
    const request = new XMLHttpRequest();
    request.onreadystatechange = () => {
      if ( request.readyState !== XMLHttpRequest.DONE ) {
        return;
      }
      if ( request.status !== 200 ) {
        this.lastError = request.statusText;
        this.fireError();
        return;
      }
      this.sessionId = JSON.parse(request.responseText);
      this.isConnected = true;
      console.log("sessionId:"+this.sessionId);
      if ( this.onopenHandler ) {
        this.fireOpen();
      }
    };

    request.open("POST", this.url, true);
    request.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
    if ( this.token )
      request.setRequestHeader("JWT", this.token );
    if ( this.uname )
      request.setRequestHeader("ID", this.uname );
    request.send("null"); // this is actually auth data currently unused. keep stuff websocket alike for now
    return this;
  }
}

class Callback {
  constructor(resultsCallback) {
    if ( ! resultsCallback ) {
      throw "must register callback before sending";
    }
    this.complete = resultsCallback;
  }
}

/**
 * A wrapper for a server side Actor
 */
class KontrActor {

  constructor( global, id, optionalType ) {
    this.global = global;
    this.id = id;
    this.type = optionalType ? optionalType : "untyped Actor";
    this.socketHolder = global.currentSocket;
  }

  /**
   * create a sequenced batch of remote calls
   */
  static buildCallList( list, seqNo ) {
    const l = list.slice();
    l.push(seqNo);
    return coder.buildJArray("array",l);
  }

  /**
   *
   * @param callbackId - callback id in case method has a promise as a result
   * @param receiverKey - target actor id
   * @param args - [] of properly formatted fst-json JavaObjects
   * @returns {{typ, obj}|*}
   */
  buildCall( callbackId, receiverKey, methodName, args ) {
    let cb = null;
    if ( args && args.lengt > 0 && args[args.length-1].typ === 'cbw' ) {
      cb = args[args.length-1];
      args[args.length-1] = null;
    }
    return coder.buildJObject( "call", { futureKey: callbackId, queue: 0, method: methodName, receiverKey: receiverKey, serializedArgs: JSON.stringify(coder.buildJArray("array",args)), cb: cb } );
  }

  static buildCallback( callbackId ) {
    return { "typ" : "cbw", "obj" : [ callbackId ] };
  }

  mapCBObjects(methodName,argList) {
    for (let i = 0; i < argList.length; i++) {
      if ( typeof argList[i] === 'function' ) { // autogenerate Callback object with given function
        argList[i] = new Callback(argList[i]);
      }
      if (argList[i] instanceof Callback) {
        const callbackId = this.global.sbIdCount++;
        this.global.futureMap[callbackId] = argList[i];
        this.global.callMap[callbackId] = [methodName,argList];
        argList[i] = this.buildCallback(callbackId);
      }
    }
  };

  sendBatched(msg,optCB) {
    this.global.batch.push(msg);
    this.global.batchCB.push(optCB);
    const socket = this.socketHolder.socket;
    socket.triggerNextSend(() => {
      //console.log("send batched \n"+JSON.stringify(batch,null,2));
      const data = JSON.stringify(this.buildCallList(batch, socket.lpSeqNo));
      const prev = batchCB;
      this.global.batch = [];
      this.global.batchCB = [];
      socket.send(data).then( (r,e) => {
        if (e) {
          this.socketHolder.socket.termOpenCBs(prev);
        }
      });
    });
  }

  /**
   * call an actor method returning a promise.
   *
   * "public IPromise myMethod( arg0, arg1, .. );"
   *
   */
  ask( methodName, args ) {
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
  }

  /**
   * call a simple asynchronous method returning nothing
   *
   * "public void myMethod( arg0, arg1, .. );"
   */
  tell( methodName, args ) {
    if ( this.socketHolder.socket === null )
      throw "not connected";
    if ( this.callCache ) { // also tell cache in order to enable proper cleaning
      this.callCache.put([methodName],null);
    }
    const argList = [];
    for ( let i = 1; i < arguments.length; i++ )
      argList.push(arguments[i]);
    this.mapCBObjects(methodName,argList);
    const msg = this.buildCall( 0, this.id, methodName, argList );
    this.sendBatched.call(this,msg);
    return this;
  }

}

if ( _kontraktor_IsNode ) {
  module.exports = {
    KClient : KClient,
    KPromise : KPromise
  };
}
