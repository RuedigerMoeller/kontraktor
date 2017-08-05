const kontraktorClient = require("kontraktor-client");
const KClient = kontraktorClient.KClient;

const kclient = new KClient();

kclient.connect("ws://localhost:3998","WS").then( (remote,e) => {

  remote.$withCallback('its me', (r, e) => {
    console.log("withCallback received ", r, e);
  });

  remote.$voidFun();

  remote.automaticPromised("Hello").then( (r,e) => console.log("autoprom",r,e));
});

