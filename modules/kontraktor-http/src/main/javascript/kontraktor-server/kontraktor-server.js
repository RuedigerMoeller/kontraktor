// enables to implement a kontraktor remote actor in javascript and run it via node
//
// currenlty only one actor can be exposed at a given port (no support for "magic" remote references to dynamic new actor's are passed to clients)
// Promises and Callbacks (must be last arg in method) are supported
//
const WebSocket = require('ws');
const kontraktor = require('kontraktor-common');

const KPromise = kontraktor.KPromise;
const DecodingHelper = kontraktor.DecodingHelper;

class KRemotableProxy {

  constructor(remotedInstance,targetInstance,javaclazznameOpt) {
    this.target=targetInstance;
    this.javaclazzname = javaclazznameOpt ? javaclazznameOpt : 'org.nustaq.kontraktor.Actor';
    this.javaclazzname += "_ActorProxy";
  }

  register(kserver,actormap) {
    this.kid = kserver.registerRef(this,actormap);
    this.actorMap = actormap; // per connection map of id => remoted actor
  }
}

class KontraktorServer {

  constructor(facade, serverOpts, decodinghelper /*opt*/) {
    this.debug = false;
    if ( !decodinghelper )
      this.decodingHelper = new DecodingHelper();
    else
      this.decodingHelper = decodinghelper;
    this.serverOpts = serverOpts;
    this._constructCall.bind(this);
    this.startServer();
    this.actorRefIdCount = 2;
    this.facade = facade;
    facade.__kserver = this;
  }

  registerRef(remotableProxy,actormap) {
    if ( remotableProxy.target == this.facade )
      throw "cannot register facade";
    let id = 0;
    Object.keys(actormap).forEach(key => {
      if (actormap[key] === remotableProxy.target)
        id = actormap[key].__proxy.kid;
    });
    if (id === 0) {
      id = this.actorRefIdCount++;
      actormap[id] = remotableProxy.target;
      remotableProxy.__kserver = this;
      remotableProxy.kid = id;
      remotableProxy.target.__proxy = remotableProxy;
      remotableProxy.target.__kserver = this;
    }
    return id;
  }

  unregisterRef(proxyOrInstance,actormap) {
    if ( proxyOrInstance == this.facade )
      throw "cannot unregister facade. close connection instead";
    let proxy = proxyOrInstance;
    if ( proxy instanceof KRemotableProxy == false) {
      proxy = proxyOrInstance.__proxy;
    }
    if ( ! proxy.target.kid ) {
      console.warn("proxy unregistered twice ",proxy);
      return;
    }
    if ( proxy.target.notifyDisconnect )
      proxy.target.notifyDisconnect(this);
    delete actormap[proxy.kid];
    proxy.target.__proxy = null;
    proxy.target.__kserver = null;
    proxy.kid = 0; // disconnected
  }

  _constructCall( actormap, calljsobj ) {
    if ( calljsobj.args.seq && calljsobj.args.seq.length === 3 && calljsobj.args.seq[1] instanceof KRemotableProxy) {
      // actor proxy
      const proxy = calljsobj.args.seq[1];
      proxy.register(this,actormap);
      calljsobj.args.seq[1] = { typ:proxy.javaclazzname, obj: [ proxy.kid, proxy.javaclazzname] };

    }
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
    let test = 1;
    this.wss = new WebSocket.Server(this.serverOpts ? this.serverOpts : { port: 8080 });
    this.wss.on('connection', function connection(ws) {
      ws.__kactormap = { 1: self.facade };
      ws.on("close", (id) => {
        if (self.facade.clientClosed) {
          self.facade.clientClosed(ws.__kactormap,id);
        }
      });
      ws.on('message', function incoming(message) {
        // FIXME: Split function
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
                  if ( this.debug )
                    console.log("remote call:", call);
                  if ( 'ask' == call.method || 'tell' == call.method ) {
                    call.method = call.args[0];
                    call.args.splice(0,1);
                  }
                  if ( call.cb ) {
                    call.args[call.args.length-1] = {
                      complete: (res,err) => {
                        this.send( JSON.stringify( self._constructCall(
                          ws.__kactormap,
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
                  let target = ws.__kactormap[call.receiverKey];
                  try {

                    var res = target[call.method].apply(target,call.args);
                    const undef = typeof res == 'undefined';
                    if ( undef && call.futureKey > 0 ) {
                      // handle call to ask on a void method
                      // send a void result to avoid memory leaks
                      res = "void";
                      console.warn("prepend '$' on void calls (method:"+call.method+")");
                    }
                    if ( res ) {
                      if ( res instanceof KPromise == false ) {
                        res = new KPromise(res);
                      }
                      res.then( (pres,perr) => {
                        this.send( JSON.stringify(
                          self._constructCall(
                            ws.__kactormap,
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
                  } catch (e) {
                    if (call.futureKey > 0) {
                      this.send(JSON.stringify(
                        self._constructCall(
                          ws.__kactormap,
                          {
                            queue: 1,
                            futureKey: call.futureKey,
                            receiverKey: call.futureKey,
                            args: {styp: 'array', seq: [2, null, call.method+":"+e ]}
                          }
                        )
                        )
                      )
                    }
                    console.error(e);
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
  KRemotableProxy : KRemotableProxy,
  KPromise : KPromise,
  KontraktorServer : KontraktorServer,
  DecodingHelper : DecodingHelper
};

