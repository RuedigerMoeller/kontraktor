const kontraktor = require("kontraktor-server");
const KPromise = kontraktor.KPromise;

class Greeter {

  greet(aString,time) {
    const res = new KPromise();
    setTimeout( () => res.complete("Hello " + aString,null), time );
    return res;
  }

}

const server = new kontraktor.KontraktorServer(new Greeter(),{port:3999});
