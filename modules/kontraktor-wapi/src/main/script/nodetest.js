// sign with default (HMAC SHA256)
var jwt = require('jsonwebtoken');
var jsk = require('/home/ruedi/projects/kontraktor/modules/kontraktor-http/src/main/javascript/js4k/js4k.js');

var token = jwt.sign(
  {
    "jti": "ID",
    "sub": "SUBJECT",
    "iss": "ISSUER"
  },
  Buffer.from('MA6cia2FyhHKy4pD8hY+8Q==', 'base64')
);
console.log("token:",token);

jsk.connect("http://localhost:7777/dummyservice", { token: token, uname: 'test' }).then(function(serv,err) {
  if ( err ) {
    console.log("error:",err);
    return;
  }
  serv.ask("service", "Rüdi").then( function(r,e) {
    console.log("Rüdi "+r+" err:"+e);
  });
  serv.tell( "subscribe", jsk.jobj("wapi.ForeignClass", { x: 3, y: 2, z: 1 }), function(r,e) {
    console.log("bcast ",r,e);
  });
});
