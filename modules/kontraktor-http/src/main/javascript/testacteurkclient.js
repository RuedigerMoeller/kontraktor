const kontraktorClient = require("kontraktor-client");

const KClient = kontraktorClient.KClient;
const KPromise = kontraktorClient.KPromise;
const coder = new kontraktorClient.DecodingHelper;

const kclient = new KClient();
// connects class TestActeur (java tests)
function sampleNoProxy(remote) {
  remote.tell("plain", "Hello", 1);
  remote.ask("plainPromise", "Hello", 1).then((r, e) => {
    console.log("plainPromise returned ", r, e);
  });
  remote.ask("plainCallback", "hello", 1, (r, e) => {
    console.log("plainCallback returned ", r, e);
  });
  let pojo = coder.jobj(
    "org.nustaq.sometest.TestPojo",
    {
      aMap: coder.jmap({"A": 1, "B": 2}),
      strings: coder.jcoll("list", ['1', '2', '3'])
    }
  );
  remote.ask("plainPojo", pojo).then((r, e) => {
    console.log("plainPojo returned ", r, e);
  });

  remote.tell("simpleTypes", ["A", "B", "C"], [1, 2, 3, -5]);
  remote.tell("plainUnknown", {
    x: "pasd",
    yyy: [1, 23, 1123, 123, 123, -1123],
    z: {text: "Halli"}
  });

  remote.ask("createAnotherOne", "otherActor").then((other, err) => {
    if (other)
      other.ask("getName").then((r, e) => console.log("othername:" + r));
    else
      console.error('failed to create dynamic actor ', other, err);
  });
}
kclient.useProxies(true).connect("http://localhost:8888/test").then( (app,e) => {
  // kclient.connect("ws://localhost:8888/test","WS").then( (app,e) => {

  //return onPlainClient(app); same code without using Proxy (see kclient.useProxies(true) lines above)

  app.plain( "Hello", 1);
  app.$plainPromise( "Hello", 1 ).then((r, e) => {
    console.log("plainPromise returned ", r, e);
  });
  app.$plainCallback( "hello", 1, (r, e) => {
    console.log("plainCallback returned ", r, e);
  });
  let pojo = coder.jobj(
    "org.nustaq.sometest.TestPojo",
    {
      aMap: coder.jmap({"A": 1, "B": 2}),
      strings: coder.jcoll("list", ['1', '2', '3'])
    }
  );
  app.$plainPojo( pojo ).then( (r, e) => {
    console.log("plainPojo returned ", r, e);
  });

  app.simpleTypes(["A", "B", "C"], [1, 2, 3, -5]);
  app.plainUnknown({
    x: "pasd",
    yyy: [1, 23, 1123, 123, 123, -1123],
    z: {text: "Halli"}
  });

  app.$createAnotherOne("otherActor").then((other, err) => {
    if (other)
      other.$getName().then((r, e) => console.log("othername:" + r));
    else
      console.error('failed to create dynamic actor ', other, err);
  });

});

