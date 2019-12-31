import 'package:kontraktor_client/synced-real-live.dart';

var feedId = "bd8d1252-979f-44ef-bd5e-d6518ff622b0";

main() async {

  SyncedRealLive rl = SyncedRealLive().init("http://localhost:8087/api","./testdata","u","pwd");
  rl.initTable("feed","feedId == '$feedId'");
  await rl.startConnection();

  SyncedRLTable sync = rl["feed"];

}
