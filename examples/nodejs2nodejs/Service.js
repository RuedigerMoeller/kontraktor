
const ks = require('kontraktor-server');

class MyService {

  greet( whom ) {
    return new ks.KPromise("Hello "+JSON.stringify(whom,2));
  }

  timeService( callback ) {
    setTimeout( () => {
      if ( ! callback.closed ) {
        callback.complete(new Date().getTime(), 'CNT' /*keep callback alive*/ );
        this.timeService(callback);
      } else {
        // client disconnected or some other party closed the callback
      }
    }, 1000 );
  }

  // notification callback
  clientClosed(map,id) {
    console.log("client closed ",map,id);
  }
}

new ks.KontraktorServer(new MyService(),{port:4999});
