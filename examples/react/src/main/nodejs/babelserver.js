const browserify = require('browserify');
const kontraktor = require("./kontraktor-server.js");

const KPromise = kontraktor.KPromise;
const DH = new kontraktor.DecodingHelper();

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

const server = new kontraktor.KontraktorServer(new Babel(),{port:3999},DH);