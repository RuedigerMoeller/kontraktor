const kontraktor = require("kontraktor-server");

const KPromise = kontraktor.KPromise;
const decodingHelper = new kontraktor.DecodingHelper();

class KServer {

  withCallback(astring,callback) {
    callback.complete("hello",'CNT');
    callback.complete(astring,'CNT');
    callback.complete(null,null);
  }

}

const server = new kontraktor.KontraktorServer(new KServer(),{port:3998},decodingHelper);