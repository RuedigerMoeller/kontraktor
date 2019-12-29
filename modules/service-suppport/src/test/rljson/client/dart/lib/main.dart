import 'dart:async';
import 'dart:io';
import 'dart:convert';
import 'package:kontraktor_client/kontraktor-client.dart';

var feedId = "bd8d1252-979f-44ef-bd5e-d6518ff622b0";

main() async {

  RLJsonSession sess = RLJsonSession("http://localhost:8087/api");
  await sess.authenticate("u", "p");
  RLTable feed = sess.createTableProxy("feed");
  TablePersistance pers = FileTablePersistance("./testdata");

  await mutateFeed(feed,false);

  SyncedRLTable sync = SyncedRLTable(feed,pers,"feedId == '$feedId'");
  await sync.init();
  sync.syncFromServer();

}

Future mutateFeed(RLTable feed, bool doprint ) async {

  feed.subscribe("feedId == '$feedId'", (r,e) async {
    if ( doprint ) print( "broadcast $r $e");
    if ( r["type"] == "QUERYDONE" ) {
      var key = uuid.v4();
      feed.updateSilent({
        "key" : key,
        "feedId" : feedId,
        "int" : 1231123,
        "dbl" : 123123.22
      });
      var ms = DateTime.now().millisecondsSinceEpoch;
      if ( doprint ) print("MS $ms");
      feed.updateSilent({
        "key" : key,
        "sampleData" : ms
      });
    }
  });

  if ( doprint ) print( await feed.fields() );
}

