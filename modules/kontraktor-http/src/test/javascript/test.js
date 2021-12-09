const kontraktorClient = require("kontraktor-client");
const KClient = kontraktorClient.KClient;

const kclient = new KClient();

kclient.connect("http://localhost:7779/httpapi","HTLP").then( (remote,e) => {
  // remote.ask("sayHello",5,(r,e) => console.log(r,e))
  const dto = {
    name: "jsname",
    value: 133,
    dvalue: 133.13,
    array: [ 11, 22, 33, "Vier4", "Fünf5" ]
  }
  // remote.testJsonArgMapped( JSON.stringify(dto) ).then( (r,e) => {
  //   console.log(JSON.parse(r),e);
  // });
  // remote.testJsonObjectArg(JSON.stringify(dto)).then( (r,e) => {
  //   console.log(JSON.parse(r),e);
  // });
  remote.testMarkedJsonArg(JSON.stringify(dto)).then( (r,e) => {
    console.log(JSON.parse(r),e);
  });

});

