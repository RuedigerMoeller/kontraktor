import 'package:kontraktor_client/synced-real-live.dart';
import 'package:uuid/uuid.dart';

var uuid = Uuid();

var feedId = "bd8d1252-979f-44ef-bd5e-d6518ff622b0";

main() async {

  SyncedRealLive rl = SyncedRealLive().init("http://localhost:8087/api","./testdata1","u","pwd");
  rl.initTable("feed","feed","feedId == '$feedId'");
  await rl.startConnection();

  SyncedRLTable sync = rl["feed"];

  print("adding ..");
  sync.addOrUpdate({
    "key" : uuid.v4(),
    "msg" : "Hello from main 1",
    "feedId" : feedId,
    "owner" : "main one"
  });

}
