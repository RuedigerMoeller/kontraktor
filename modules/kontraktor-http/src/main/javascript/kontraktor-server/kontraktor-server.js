// enables to implement a kontraktor remote actor in javascript and run it via node
//
// currenlty only one actor can be exposed at a given port (no support for "magic" remote references to dynamic new actor's are passed to clients)
// Promises and Callbacks (must be last arg in method) are supported
//
const WebSocket = require('ws');
const kontraktor = require('kontraktor-common');

const KPromise = kontraktor.KPromise;
const DecodingHelper = kontraktor.DecodingHelper;

class KontraktorServer {

  constructor(facade, serverOpts, decodinghelper /*opt*/) {
    this.debug = false;
    if ( !decodinghelper )
      this.decodingHelper = new DecodingHelper();
    else
      this.decodingHelper = decodinghelper;
    this.actormap = { 1: facade };
    this.serverOpts = serverOpts;
    this._constructCall.bind(this);
    this.startServer();
  }

  _constructCall( calljsobj ) {
    return {
      styp: 'array',
      seq: [
        2,
        this.decodingHelper.jobj( "call",calljsobj),
        0
      ]};
  }

  startServer() {
    const self = this;
    this.wss = new WebSocket.Server(this.serverOpts ? this.serverOpts : { port: 8080 });
    this.wss.on('connection', function connection(ws) {

      ws.on('connect', function incoming(message) {
        const msg = JSON.parse(message);
        if ( this.debug ) console.log('connect:', msg);
      }.bind(this));

      ws.on('message', function incoming(message) {
        try {
          const msg = JSON.parse(message);
          if ( this.debug )
            console.log('message:', msg);
          if ( msg.styp === 'array' ) {
            const msgarr = msg.seq;
            if ( msgarr.length > 1) {
              for ( var i = 1; i < msgarr.length-1; i++ ) {
                if ( msgarr[i].typ === 'call' ) {
                  let call = msgarr[i].obj;
                  if ( call.serializedArgs != null ) {
                    const args = self.decodingHelper.transformJavaJson(JSON.parse(call.serializedArgs),true);
                    call.args = args;
                    call.serializedArgs = null;
                  } else {
                    call.args = self.decodingHelper.transformJavaJson(call.args,true);
                  }
                  if ( this.debug ) console.log("remote call:", call);
                  if ( call.cb ) {
                    call.args[call.args.length-1] = {
                      complete: (res,err) => {
                        this.send( JSON.stringify( self._constructCall(
                          {
                            queue: 1,
                            futureKey: call.cb.obj[0],
                            receiverKey: call.cb.obj[0],
                            args: { styp: 'array', seq: [2, res, err ] },
                            isContinue: err === 'CNT'
                          }
                        )));
                      }
                    };
                  }
                  let target = self.actormap[call.receiverKey];
                  var res = target[call.method].apply(target,call.args);
                  const undef = typeof res == 'undefined';
                  if ( undef && call.futureKey > 0 ) {
                    // handle call to ask on a void method
                    // send a void result send to avoid memory leaks
                    res == "void";
                    console.warn("prepend '$' on void calls (method:"+call.method+")");
                  }
                  if ( res ) {
                    if ( res instanceof KPromise == false ) {
                      res = new KPromise(res);
                    }
                    res.then( (pres,perr) => {
                      this.send( JSON.stringify(
                        self._constructCall(
                          {
                            queue: 1,
                            futureKey: call.futureKey,
                            receiverKey: call.futureKey,
                            args: { styp: 'array', seq: [2, pres, perr ] }
                          }
                        )
                        )
                      )});
                  }
                }
              }
            }
          }
        } catch (e) {
          console.error(e);
        }}
      );
    });
  }

}

module.exports = {
  KPromise : KPromise,
  KontraktorServer : KontraktorServer,
  DecodingHelper : DecodingHelper
};

