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

  // returns server timestamp { key: [ {addObj}, {addObj] ]
  bulkUpdate( addOrUpdateObject ) {
    return toES6Prom(this.session.ask("bulkUpdate",this.table,JSON.stringify(addOrUpdateObject)));
  }

  get( key ) {
    return toES6Prom(this.session.ask("get",this.table,key));
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
  const feed = new Table(session,"feed");
  const x = await feed.fields();
  console.log("fields",x);
  try {
    let serverTS = await feed.bulkUpdate({
      "TestIncrementals2" : [
//        { "array-": "Hello1" },
        { "array-+": "Hello1" },
      ]
    });
    let rec = await feed.get("TestIncrementals2");
    console.log("rec",rec);
  } catch (e) {
    console.error(e);
  }
}

const kclient = new k.KClient().useProxies(false);
const url = "ws://localhost:8087/ws";

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
