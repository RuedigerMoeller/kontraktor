import 'dart:async';
import 'dart:convert';
import 'kontraktor-client.dart';

class RLJsonSession {
  KontraktorConnection con;
  RemoteActor session;
  int senderId;
  String url;

  RLJsonSession(String url) {
    con = KontraktorConnection(url);
  }

  Future<bool> authenticate( String user, String pwd ) async {
    if ( con.isDisconnected() ) {
      await con.connect();
      print("connected");
    }
    var login = await con.ask("authenticate", [ user, pwd ] );
    session = login["session"];
    senderId = await session.ask("getSenderId",[]);
    print("sessionId ${con.sid}, senderId $senderId");
    return true;
  }

  RLTable createTableProxy(String name) {
    return RLTable(this,name);
  }
}

class RLTable {

  RLJsonSession jsonsession;
  RemoteActor session;
  String name;

  RLTable(RLJsonSession session,String table) {
    this.jsonsession = session;
    this.session = jsonsession.session;
    this.name = table;
  }

  Future<Map> update( Map jsonObject ) {
    return session.ask("update", [name,jsonEncode(jsonObject)]);
  }

  updateSilent( Map jsonObject ) {
    session.tell("updateAsync",[name,jsonEncode(jsonObject)]);
  }

  Future<bool> delete(key) {
    return session.ask("delete",[this.name,key]);
  }

  deleteSilent(key) {
    this.session.tell("deleteAsync",[name,key]);
  }

  Future<List>fields() async {
    var res = await session.ask("fieldsOf", [name]);
    return res;
  }

  selectAsync(String query, Function(dynamic,dynamic) cb) {
    session.tell("select", [name, query, (r,e) {
      if ( r != null )
        cb( jsonDecode(r), null );
      else
        cb(r,e);
    }]);
  }

  // returns array of result objects
  Future<List<Map>> select(String query) async {
    Completer comp = Completer<List<Map>>();
    var res = List<Map>();
    selectAsync(query, (r,e) {
      if ( r != null ) {
        res.add(r);
      }
      else if ( e != null ) {
        comp.completeError(e);
      } else {
        comp.complete(res);
      }
    });
    return comp.future;
  }

  String subscribe(String query, Function(dynamic,dynamic) cb) {
    String id = uuid.v4();
    session.tell("subscribe", [id, this.name, query, (r,e) {
      if ( r != null ) {
        cb(jsonDecode(r),e);
      }
      else {
        cb(r,e);
      }
    }]);
    return id;
  }

  /**
   * returns [ subsid (for unsubscribing), timestamp to be used for next syncing subscriptions]
   * timestamp can be updated if records with higher lasteModified come in
   */
  Future<List> subscribeSyncing(int timeStamp, String query, Function(dynamic,dynamic) cb) async {
    String subsid = uuid.v4();
    int timestamp = await session.ask("subscribeSyncing", [subsid, this.name, timeStamp, query, (r,e) {
      if ( r != null ) {
        cb(jsonDecode(r),e);
      }
      else {
        cb(r,e);
      }
    }]);
    return [subsid,timestamp];
  }

  String unsubscribe( String id ) {
    session.tell("unsubscribe", [id]);
  }

  Future<bool> bulkUpdate(lts) {
    session.ask("bulkUpdate",[name,jsonEncode(lts)]);
  }

}
