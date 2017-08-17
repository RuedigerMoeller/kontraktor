// ES6 Port of js4k 3.34
// matches kontraktor 3 json-no-ref encoded remoting

const _kontraktor_IsNode = Object.prototype.toString.call(typeof process !== 'undefined' ? process : 0) === '[object process]';
if ( _kontraktor_IsNode ) {
  kontraktor = require('kontraktor-common');

  KPromise = kontraktor.KPromise;
  coder = new kontraktor.DecodingHelper();
  if ( typeof window === 'undefined')
    window = {}; // node
  XMLHttpRequest = require("./xmlhttpdummy.js");
  WebSocket = require('ws');
} else {
  coder = new DecodingHelper();
}

const NO_RESULT = "NO_RESULT";

class KClientListener {
  // if server does not support resurrection this signals session timeout / connection loss
  onInvalidResponse(response) {
    console.error("invalid response")
  }
  onError(obj) {
    console.error("connectionError",obj)
  }
  onClosed() {
    console.warn("connection closed")
  }
  onResurrection() {
    console.log("session resurrected")
  }
}

// a (batching) kontraktor client
class KClient {

  constructor() {
    this.reset();
    this.listener = new KClientListener();
  }

  reset() {
    this.currentSocket = {socket: null};
    this.futureMap = {}; // future id => promise
    this.callMap = {}; // future id => argumentlist to support offline caching
    this.currentSocket = { socket: null }; // use indirection to refer to a socket in order to ease reconnects
    this.sbIdCount = 1;
    this.batch = [];
    this.batchCB = [];
    this.proxies = true;
    this.listener = new KClientListener();
  }
  // if set to false => only tell, ask style calls are allowed, else a proxy is generated which
  // generates tell/ask messages from methods called on the proxy. Use x.$methodname() if the method returns a promise (=ask).
  useProxies(bool) {
    this.proxies = bool;
    return this;
  }
  connect(wsurl, connectionMode) {
    const res = new KPromise();
    if ( this.currentSocket.socket != null ) {
      res.complete(this.remoteApp,null);
      return res;
    }
    let socket = null;
    if ( 'WS' === connectionMode ) {
      socket = new KontraktorSocket(this,wsurl);
    } else if ( "HTLP" === connectionMode || "HTTP" === connectionMode ) {
      socket = new KontraktorPollSocket(this,wsurl, "HTLP" === connectionMode );
    } else if ( connectionMode && connectionMode.uname && connectionMode.token ) { // token
      socket = new KontraktorPollSocket(this,wsurl, true,connectionMode.token, connectionMode.uname );
    } else {
      console.log("unknown connectionMode, default to HTLP");
      socket = new KontraktorPollSocket(this,wsurl, true );
    }
    const myHttpApp = new KontrActor(this,1,"RemoteApp");
    socket.onmessage( message => {
      console.error("unexpected message");
      console.log(JSON.stringify(message, null, 2));
      this.listener.onError(message);
    });
    socket.onerror( err => {
      this.listener.onError(err);
      console.log(err);
      if ( ! res.isCompleted() )
        res.complete(null,err);
    });
    socket.onclose( () => {
      this.socket = null;
      if ( ! res.isCompleted() )
        res.complete(null,"closed");
      this.listener.onClosed();
      console.log("closed connection");
    });
    socket.onopen( event=> {
      this.currentSocket.socket = socket;
      this.remoteApp = myHttpApp;
      res.complete( this.proxies ? createActorProxy(myHttpApp) : myHttpApp ,null);
    });
    return res;
  };

  processSocketResponse(lastSeenSequence, decodedResponse, automaticTransformResults, messageListener) {
    const respLen = decodedResponse.seq[0] - 1; // last one is sequence.
    const sequence = decodedResponse.seq[decodedResponse.seq.length-1];
    //console.log("GOT SEQUENCE:"+sequence);
    if ( sequence > 0 && sequence <= lastSeenSequence ) {
      console.log("old data received:"+sequence+" last:"+lastSeenSequence);
      // return lastSeenSequence;
    }
    //console.log("resplen:"+respLen);
    for (let i = 0; i < respLen; i++) {
      const resp = decodedResponse.seq[i + 1];
      if ( ! resp.obj ) {
        this.listener.onInvalidResponse(resp);
        continue;
      }
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
                let kontrActor = new KontrActor(this, obj[0], obj[1]);
                return this.proxies ? createActorProxy( kontrActor ) : kontrActor; // automatically create remote actor wrapper
              }
              return null;
            };
            const res = coder.transformJavaJson(resp.obj.args.seq[1], true, transFun);
            const err = coder.transformJavaJson(resp.obj.args.seq[2], transFun);
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
        if (_kontraktor_IsNode) {
          const response = JSON.parse(message.data);
          this.global.processSocketResponse(-1,response, this.automaticTransformResults, eventListener.bind(this));
          return;
        }
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
    this.onopenHandler.apply(this, [{event: "opened", session: this.sessionId}]);
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
      this.onerrorHandler.apply(this, [{event: "connection failure", status: this.lastError}]);
    }
  }

  onopen(eventHandler) {
    this.onopenHandler = eventHandler;
    if ( this.sessionId ) {
      this.fireOpen();
    }
    setTimeout(this.longPoll.bind(this),0);
  };

  longPoll() {
    const sleepNoReqSent = 100;
    if ( this.doStop || ! this.doLongPoll ) {
      console.log("lp stopped");
      return;
    }
    if ( ! this.isConnected ) {
      setTimeout(this.longPoll.bind(this),sleepNoReqSent);
    } else {
      const cblen = Object.keys(this.global.futureMap).length;
      if ( cblen === 0 ) {
        // in case no pending callback or promise is present => skip LP
        setTimeout(this.longPoll.bind(this),sleepNoReqSent);
        return;
      }
      console.log("futureMap SIZE ON LP *** ", cblen );
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
          setTimeout(this.longPoll.bind(this),3000);
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
            // console.log("resp is empty");
            setTimeout(this.longPoll.bind(this),200);
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
      this.global.listener.onResurrection();
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
            if (respObject.seq[1].indexOf("Unknown actor") == 0) {
              this.global.listener.onInvalidResponse(respObject.seq[1])
            }
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
        tmp.apply(this,[]);
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

const createActorProxy = function(target){
  return new Proxy(target,{
    // apply: function(target, thisArg, argumentsList) {
    //   console.log("funcall ",target,thisArg,argumentsList);
    // },
    get: function(target, property, receiver) {
      return function () {
        if ("target" === property)
          return target;
        if ( "ask" === property )
          return target.ask.apply(target,arguments);
        if ( "tell" === property ) {
          target.tell.apply(target,arguments);
          return null;
        }
        let args = [property];
        for (let i = 0; i < arguments.length; i++) {
          args.push(arguments[i]);
        }
        if (property.indexOf("$") === 0) {
          args[0] = args[0].substring(1);
          target.tell.apply(target, args);
        }
        else {
          return target.ask.apply(target, args);
        }
        return null;
      }
    }
  });
};

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
  buildCallList( list, seqNo ) {
    const l = list.slice();
    l.push(seqNo);
    return coder.jarray("array",l);
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
    if ( args && args.length > 0 && args[args.length-1] && args[args.length-1].typ === 'cbw' ) {
      cb = args[args.length-1];
      args[args.length-1] = null;
    }
    return coder.jobj( "call", { futureKey: callbackId, queue: 0, method: methodName, receiverKey: receiverKey, serializedArgs: JSON.stringify(coder.jarray("array",args)), cb: cb } );
  }

  buildCallback( callbackId ) {
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
      const data = JSON.stringify(this.buildCallList(this.global.batch, socket.lpSeqNo));
      const prev = this.global.batchCB;
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
    if ( this.global.callCache && this.global.callCache.getCached ) {
      var res = this.global.callCache.getCached( methodName,arguments );
      if ( res !== NO_RESULT ) {
        var cbr = new KPromise();
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
    const futID = this.global.sbIdCount++;
    const cb = new KPromise();
    this.global.futureMap[futID] = cb;
    this.global.callMap[futID] = [methodName,argList];
    var msg = this.buildCall( futID, this.id, methodName, argList );
    this.sendBatched(msg,futID);
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
    KPromise : KPromise,
    DecodingHelper : kontraktor.DecodingHelper,
    KClientListener : KClientListener
  };
}
