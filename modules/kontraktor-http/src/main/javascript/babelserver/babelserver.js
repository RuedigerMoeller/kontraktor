const browserify = require('browserify');
const kontraktor = require("kontraktor-server");

const KPromise = kontraktor.KPromise;
const decodingHelper = new kontraktor.DecodingHelper();

class Babel {

  browserifyInternal(filePath,babelopts) {
    try {
      const prom = new KPromise();
      var res = browserify(filePath, {
        debug: !!babelopts.debug
        // paths: ['/home/ruedi/IdeaProjects/kontraktor/examples/react/src/main/nodejs/']
      })
      .transform("babelify", { presets: babelopts.presets, extensions: [".tsx", ".ts", ".js", ".jsx"] })
      .bundle( (err,buff) => {
        if ( err ) {
          console.log(err);
          prom.complete(decodingHelper.jobj("BabelResult",{ code: null, err: ''+err }),null);
        } else
          prom.complete(decodingHelper.jobj("BabelResult",{ code: ""+buff, err: null }),null);
      });
      return prom;
    } catch (e) {
      console.error(e);
    }
  }

}

const server = new kontraktor.KontraktorServer(new Babel(),{port:3999},decodingHelper);