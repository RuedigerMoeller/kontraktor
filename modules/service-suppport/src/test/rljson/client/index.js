const k = require('kontraktor-client');

function toES6Prom(kpromise) {
  return new Promise((resolve, reject) => kpromise.then((r, e) => e ? reject(e) : resolve(r)));
}

class Table {
  
  constructor(session,table) {
    this.session = session;
    this.table = table;
  }

  update( json ) {
    return toES6Prom(this.session.ask("update",this.table,JSON.stringify(json)));
  }
  
}

function testSession(session) {
  const creds = new Table(session,"credentials");
  creds.update( {
    key: '0x0x0x0x0',
    name: 'Me',
    array: [ 1, 2, 3, { x: 123.2 }, "hi", true ],
    sub: {
      oha: 'ne', tt: 13.22
    }
  })
  .then( r => console.log("updated") )
  .catch( e => console.log("erro") );
}

const kclient = new k.KClient().useProxies(false);
const url = "ws://localhost:8081/ws";

kclient.connect(url, "WS")
.timeoutIn(10000)
.then( (server,error) => {
  if ( server ) {
    server.ask("authenticate","user","pwd")
    .then( (res,error) => {
      if ( res ) {
        console.log(res);
        testSession(res.session);
      } else {
        console.error(error);
      }
    });
  } else {
    console.error(error);
  }
});
