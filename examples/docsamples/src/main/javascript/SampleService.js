const kontraktor = require("kontraktor-server");

const KPromise = kontraktor.KPromise;
const KRemotableProxy = kontraktor.KRemotableProxy;
const decodingHelper = new kontraktor.DecodingHelper();

class KServer {

  constructor() {
    this.singletonSubservice = new Subservice();
  }

  withCallback(astring, callback) {
    callback.complete("hello",'CNT');
    callback.complete(astring,'CNT');
    callback.complete(null,null);
  }

  withCallbackAndPromise(astring, callback) {
    callback.complete("cb+prom hello",'CNT');
    callback.complete("cb+prom "+astring,'CNT');
    callback.complete(null,null);
    return "unrelated result";
  }

  voidFun() {
    console.log("void")
  }

  /**
   * KPromise works differently than es6 promises .. sorry folks ;)
   * @param aString
   */
  withPromise(aString) {
    const res = new KPromise();
    setTimeout( () => res.complete("you sent:" + aString,null), 1000 );
    return res;
  }

  /**
   * for kontraktor-javascript, a promise is generated automatically if a non-promise is returned
   * So you only need to deal with KPromise for methods performing asynchronous operations
   * such as a database IO or async file IO
   * @param aString
   * @returns {*}
   */
  automaticPromised(aString) {
    return aString+" was received";
  }

  getSingletonSubservice(credentials) {
    if ( "lol" == credentials ) {
      return new KRemotableProxy(this,this.singletonSubservice);
    }
    throw new Error("invalid credentials");
  }

  getSingletonSubserviceTyped(credentials) {
    if ( "lol" == credentials ) {
      return new KRemotableProxy(this,this.singletonSubservice,"docsamples.jsinterop.typedclient.ISampleSubService");
    }
    throw new Error("invalid credentials");
  }

  clientClosed(id) {
    console.log('client closed',id);
  }
}

class Subservice {

  withCallbackAndPromise(astring, callback) {
    callback.complete("subservice cb+prom hello",'CNT');
    callback.complete("subservice cb+prom "+astring,'CNT');
    callback.complete(null,null);
    return "subservice promise result";
  }

  voidFun() {
    console.log("void")
  }

}

const server = new kontraktor.KontraktorServer(new KServer(),{port:3998},decodingHelper);
