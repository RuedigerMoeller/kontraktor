import 'dart:async';
import 'dart:io';
import 'dart:convert';
import 'package:kontraktor_client/kontraktor-client.dart';

var feedId = "bd8d1252-979f-44ef-bd5e-d6518ff622b0";

main() async {

  RLJsonSession sess = RLJsonSession("http://localhost:8087/api");
  await sess.authenticate("u", "p");
  RLTable feed = sess.createTableProxy("feed");

  var res = await feed.select("feedId == '$feedId'");

  var ms = DateTime.now().millisecondsSinceEpoch;

  // add
  var key = uuid.v4();
  feed.updateSilent({
    "key" : key,
    "feedId" : feedId,
    "int" : ms,
    "dbl" : 123123.22,
    "str" : "Hello"
  });

  Future.delayed(Duration(seconds: 1), () {
    // update
    feed.updateSilent({
      "key" : res[0]["key"],
      "sampleData" : ms
    });
  });

  Future.delayed(Duration(seconds: 2), ()
  {
    // virtual del
    feed.updateSilent({
      "key": res[0]["key"],
      "_del": true
    });
  });

}


