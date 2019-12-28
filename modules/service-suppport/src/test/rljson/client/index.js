const k = require('kontraktor-client');
const uuid = require('uuid/v1');

function toES6Prom(kpromise, mapfun = x => x) {
  return new Promise((resolve, reject) => kpromise.then((r, e) => e ? reject(e) : resolve(mapfun(r))));
}

class Table {
  
  constructor(session,table) {
    this.session = session;
    this.table = table;
  }

  update( json ) {
    return toES6Prom(this.session.ask("update",this.table,JSON.stringify(json)));
  }
  
  updateAsync( json ) {
    this.session.tell("updateAsync",this.table,JSON.stringify(json));
  }

  delete(key) {
    return toES6Prom(this.session.ask("delete",this.table,key));
  }
  
  deleteAsync(key) {
    return toES6Prom(this.session.ask("deleteAsync",this.table,key));
  }

  fields() {
    return toES6Prom(this.session.ask("fieldsOf",this.table), x => {
      delete x._typ;
      return x;
    });
  }
  
  selectAsync(query,cb) {
    this.session.tell("select",this.table,query, (r,e) => {
      if ( r ) cb( JSON.parse(r), null );
      else cb(r,e);
    });
  }
 
  // returns arry of result objects
  async select(query,cb) {
    return await new Promise( (resolve,reject) => {
      const arr = [];
      this.selectAsync(query, (r,e) => {
        if ( r ) {
          arr.push(r);
        }
        else if ( e ) {
          reject(e);
        }
        else {
          resolve(arr);
        }
      })
    });
  }
  
  subscribe(query,cb,errorCallback) {
    let id = uuid();
    this.session.tell("subscribe", id, this.table, query, (r,e) => {
        if ( r ) {
          cb(JSON.parse(r));
        }
        else {
          if ( errorCallback )
            errorCallback(e);
          else
            console.error(e);
        }
      });
    return id;
  }

}

async function testSession(session) {
  const creds = new Table(session,"credentials");
  // for ( var i=0; i < 30; i++ ) {
  //   creds.update( {
  //     key: Math.random()+'--'+i,
  //     aName: 'Me'+i,
  //     pastName: 'trollo'+i,
  //     anArray: [ 5, 2, 3, { x: 123.2 }, "hi", true ],
  //     aSub: {
  //       oha: 'ne'+i, tt: 13.22, test: 'x', time: new Date().getTime(),
  //     }
  //   })
  //   .then( r => console.log("updated") )
  //   .catch( e => console.log("error", e) );
  // }
  const x = await creds.fields();
  console.log("fields",x);
  try {
//    const arr = await creds.select("(aName ** '15' || aName ** '13') && !exists(pastName) && exists(aSub.test)");
//     const arr = await creds.select("aName ** 'me15' && aSub.time > age(1,'min')");
    const arr = await creds.select("aSub.time > age(10000,'sec') && anArray ** true");
    arr.forEach( x => console.log(x) );
    
    creds.subscribe("aName ** 'you'", change => {
      console.log("CHANGE",change);
    });
    
    setTimeout( () => {
      creds.update( {
          key: arr[0].key,
          aName: 'you'+Math.random()
        }
      );
    }, 2000);
    setTimeout( () => {
      creds.update( {
          key: arr[0].key,
          aName: 'you'+Math.random(),
          aSub: { ...arr[0].aSub, oha: "anders" }
        }
      );
    }, 2000);
  } catch (e) {
    console.error(e);
  }
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
