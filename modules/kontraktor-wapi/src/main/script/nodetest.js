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

jsk.connect("http://localhost:7777/api", { token: token, uname: 'test' }).then(function(serv,err) {
  if ( err ) {
    console.log("error:",err);
    return;
  }
  serv.ask("hello", "Rüdi").then( function(r,e) {
    console.log("hello "+r+" err:"+e);
  });
  serv.ask("verify", token).then( function(rr,ee) {
    console.log("verify "+rr+" err:"+ee);
  });
  serv.tell("cyclicPing", function(rr,ee) {
    console.log("cyclicPing "+rr);
  });
  for ( var i = 0; i < 7; i++ ) {
    serv.tell("load");
  }
});
