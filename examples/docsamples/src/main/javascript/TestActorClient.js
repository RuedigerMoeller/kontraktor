// Assumes "TestActor" is already running

const kontraktorClient = require("kontraktor-client");

const KClient = kontraktorClient.KClient;
const KPromise = kontraktorClient.KPromise;
const coder = new kontraktorClient.DecodingHelper();
const kclient = new KClient();

kclient.useProxies(true).connect("http://localhost:8888/test").then( (app,e) => {
  app.$plain( "Hello", 1);

  app.getName().then((r, e) => {
    console.log("facade name:", r, e);
  });

  app.plainPromise( "Hello", 1 ).then((r, e) => {
    console.log("plainPromise returned ", r, e);
  });

  app.plainCallback( "hello", 1, (r, e) => {
    console.log("plainCallback returned ", r, e);
  });
  // construct java deserializable Pojo (see below)
  const pojo = coder.jobj(
    "docsamples.jsinterop.javaserves.TestPojo",
    {
      aMap: coder.jmap({"A": 1, "B": 2}),
      strings: coder.jcoll("list", ['1', '2', '3'])
    }
  );
  app.plainPojo( pojo ).then( (r, e) => {
    console.log("plainPojo returned ", r, e);
  });

  app.$simpleTypes(["A", "B", "C"], [1, 2, 3, -5]);

  app.$plainUnknown({
    x: "pasd",
    yyy: [1, 23, 1123, 123, 123, -1123],
    z: {text: "Halli"}
  });

  app.createAnotherOne("otherActor").then((other, err) => {
    if (other)
      other.getName().then((r, e) => console.log("othername:" + r));
    else
      console.error('failed to create dynamic actor ', other, err);
  });
});
