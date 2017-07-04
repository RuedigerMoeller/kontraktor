const bylon = require("babel-standalone");
const browserify = require('browserify');
const KS = require("./kontraktor-server.js");

const KPromise = KS.KPromise;
const DH = new KS.DecodingHelper();

class Babel {

  browserify(filePath) {
    const prom = new KPromise();
    var res = browserify(filePath)
      .transform("babelify", { presets: ["import-export", "react"] })
      .bundle( (err,buff) => {
        if ( err ) {
          console.log(err);
          prom.complete(DH.jobj("BabelResult",{ code: null, err: JSON.stringify(err) }),null);
        } else
          prom.complete(DH.jobj("BabelResult",{ code: ""+buff, err: null }),null);
      });
      return prom;
  }

}

const server = new KS.KontraktorServer(new Babel(),{port:3999},DH);