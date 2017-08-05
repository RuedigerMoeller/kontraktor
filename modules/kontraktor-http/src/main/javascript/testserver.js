const kontraktor = require("kontraktor-server");

const KPromise = kontraktor.KPromise;
const decodingHelper = new kontraktor.DecodingHelper();

class KServer {

  withCallback(astring,callback) {
    callback.complete("hello",'CNT');
    callback.complete(astring,'CNT');
    callback.complete(null,null);
  }

  voidFun() {
    console.log("void")
  }

  automaticPromised(aString) {
    return aString;
  }
}

const server = new kontraktor.KontraktorServer(new KServer(),{port:3998},decodingHelper);
