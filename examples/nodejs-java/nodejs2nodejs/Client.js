const kc = require('kontraktor-client');

new kc.KClient()
  .connect("ws://localhost:4999","WS").then( (service,error) => {
    if ( service != null ) {
      service.$timeService( (res,err) => {
        if ( res != null )
          console.log( new Date(res) );
      });
      service.greet( { json:[ 1,2,3,4 ], object: { "test":"xx" } } )
        .then( (r,e) => console.log("greet returned:", r,e));
    } else
      console.error(error);
  });
