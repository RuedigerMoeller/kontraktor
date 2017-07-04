const browserify = require('browserify');
const KS = require("./kontraktor-server.js");

const KPromise = KS.KPromise;
const DH = new KS.DecodingHelper();

class Babel {

  browserifyInternal(filePath) {
    try {
      const prom = new KPromise();
      var res = browserify(filePath, {
        // paths: ['/home/ruedi/IdeaProjects/kontraktor/examples/react/src/main/nodejs/']
      })
      .transform("babelify", { presets: [ "import-export", "react" ] })
      .bundle( (err,buff) => {
        if ( err ) {
          console.log(err);
          prom.complete(DH.jobj("BabelResult",{ code: null, err: ''+err }),null);
        } else
          prom.complete(DH.jobj("BabelResult",{ code: ""+buff, err: null }),null);
      });
      return prom;
    } catch (e) {
      console.error(e);
    }
  }

}

const server = new KS.KontraktorServer(new Babel(),{port:3999},DH);