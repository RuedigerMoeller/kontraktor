const kontraktorClient = require("./kontraktor-client");
const KClient = kontraktorClient.KClient;
const KPromise = kontraktorClient.KPromise;

const kclient = new KClient();
kclient.connect("http://localhost:8888/test").then( (remote,e) => {
  remote.tell("plain","Hello",1);
});

