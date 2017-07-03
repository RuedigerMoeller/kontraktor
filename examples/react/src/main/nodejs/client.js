const WebSocket = require('ws');

const wss = new WebSocket.Server({ port: 3999 });

wss.on('connection', function connection(ws) {

  // console.log("connection ",ws);

  ws.on('connect', function incoming(message) {
    const msg = JSON.parse(message);
    console.log('connect:', msg);
  });

  ws.on('message', function incoming(message) {
    const msg = JSON.parse(message);
    console.log('message:', msg);
    if ( msg.styp === 'array' ) {
      const msgarr = msg.seq;
      if ( msgarr.length > 1) {
        for ( var i = 1; i < msgarr.length-1; i++ ) {
          console.log("remote call:", msgarr[i]);
        }
      }
    }
  });

});
