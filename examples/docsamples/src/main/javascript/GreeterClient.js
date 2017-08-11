const kontraktorClient = require("kontraktor-client");
const kclient = new kontraktorClient.KClient();

kclient.connect("ws://localhost:3999","WS").then( (remote,e) => {

  remote.greet('Kontraktor',1000).then( (r, e) =>
    console.log("received: '"+r+"'")
  );

});

