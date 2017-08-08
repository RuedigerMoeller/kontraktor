const kontraktorClient = require("kontraktor-client");
const kclient = new kontraktorClient.KClient();

kclient.connect("http://localhost:8888/test").then( (remote,e) => {

  remote.greet('Kontraktor',1000).then( (r, e) =>
    console.log("received: '"+r+"'")
  );

});

