import '../lib/kontraktor-client.dart';

void main() async {
  KontraktorConnection con = KontraktorConnection("http://localhost:8087/api");
  await con.connect();
  print( "res0 "+await con.ask("test", ["pokpok"]) );
  print( "res1 "+await con.ask("test", ["pokpok1"]) );
  print( "res2 "+await con.ask("test", ["pokpok2"]) );

  print( "test2 "+await con.ask("test1",
    [
      "pokpok2",
      (res,e) {
        print("cb test2 $res $e");
      }
    ]
  )
  );
  var login = await con.ask("login", ["xx@xx.xx","qweqwe", (res,err) async {
    print( "Session CB received:$res $err" );
  }]);
  var sess = login["session"];
  String res1 = await sess.ask( "hello", ["pokpok"] );
  print( "res: $res1" );

  print( con.sid );
}
