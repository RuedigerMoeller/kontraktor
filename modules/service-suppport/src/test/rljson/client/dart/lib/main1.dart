import 'package:kontraktor_client/synced-real-live.dart';
import 'package:kontraktor_client/real-live.dart';

var feedId = "bd8d1252-979f-44ef-bd5e-d6518ff622b0";

main() async {

  RLJsonSession sess = RLJsonSession("http://localhost:8087/api");
  await sess.authenticate("u", "p");
  RLTable feed = sess.createTableProxy("feed");
  TablePersistance pers = FileTablePersistance("./testdata1");

  SyncedRLTable sync = SyncedRLTable(feed,pers,"feedId == '$feedId'");
  await sync.init();
  sync.syncFromServer();

  sync.addOrUpdate({
    "key" : uuid.v4(),
    "msg" : "Hello from main 1",
    "feedId" : feedId,
    "owner" : "main one"
  });

}
