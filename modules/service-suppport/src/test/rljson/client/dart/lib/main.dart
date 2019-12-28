import 'dart:async';
import 'dart:io';
import 'dart:convert';
import 'package:kontraktor_client/kontraktor-client.dart';

main() async {

  RLJsonSession sess = RLJsonSession("http://localhost:8087/api");
  await sess.authenticate("u", "p");
  RLTable feed = sess.createTableProxy("feed");

  var res = await feed.select("true");
  String feedId = uuid.v4();
  if ( res.length > 0 )
    feedId = res[0]["feedId"];

  feed.subscribe("feedId == '$feedId'", (r,e) async {
    print( "broadcast $r $e");
    if ( r["type"] == "QUERYDONE" ) {
      var key = uuid.v4();
      feed.updateSilent({
        "key" : key,
        "feedId" : feedId
      });
      var ms = DateTime.now().millisecondsSinceEpoch;
      print("MS $ms");
      feed.updateSilent({
        "key" : key,
        "sampleData" : ms
      });
    }
  });

  print( await feed.fields() );


}

