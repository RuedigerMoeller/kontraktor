const browserify = require('browserify');
const kontraktor = require("kontraktor-server");

const KPromise = kontraktor.KPromise;
const decodingHelper = new kontraktor.DecodingHelper();

class Babel {

  browserifyInternal(filePath,babelopts) {
    const prom = new KPromise();
    try {
      var res = browserify(filePath, {
        debug: !!babelopts.debug
      })
      .transform("babelify", { presets: babelopts.presets, extensions: [".tsx", ".ts", ".js", ".jsx"] })
      .bundle( (err,buff) => {
        if ( err ) {
          console.log(err);
          prom.complete(decodingHelper.jobj("BabelResult",{ code: null, err: ''+err }),null);
        } else
          prom.complete(decodingHelper.jobj("BabelResult",{ code: ""+buff, err: null }),null);
      });
    } catch (e) {
      console.error(e);
      prom.complete(null,e)
    }
    return prom;
  }

}

const server = new kontraktor.KontraktorServer(new Babel(),{port:3999},decodingHelper);