import 'package:http/http.dart' as http;
import 'dart:convert';
import 'dart:async';
import 'package:uuid/uuid.dart';

var uuid = Uuid();

class KontraktorConnection {

  String url;
  String sid;
  bool disconnect = false;
  List outqueue = [];
  int cbidCount = 1;
  int actorId = 1;
  Map<int,dynamic> cbMap = {};

  KontraktorConnection(url) {
    this.url = url;
  }

  bool isDisconnected() {
    return sid == null;
  }

  Future<bool> connect() async {
    try {
      var response = await http.post(url);
      if (response.statusCode == 200) {
        sid = jsonDecode(response.body);
        longPoll();
        return true;
      }
    } catch (e,t) {
      print("$e $t");
    }
    return false;
  }

  longPoll() async {
    while ( ! disconnect ) {
      var calls = { "styp" : "array", "seq": [ 1, 1 /* should be sequence*/]};
      var body = jsonEncode(calls);
      var response = await http.post( url, headers: { "sid": sid }, body: body );
//      print("resp:"+response.body);
      if ( response.body != "") {
        var resp = jsonDecode(response.body);
        var transFormed = transformJavaJson(resp);
        for (var i = 0; i < transFormed.length-1; ++i) {
          var call = transFormed[i];
          call["args"] = transformJavaJson(jsonDecode(call["serializedArgs"]));
          //print( call );
          var recKey = call["receiverKey"];
          if ( recKey != 0 ) {
            var promiseCB = cbMap[recKey];
            if ( promiseCB == null ) {
              print("unknown callback for: "+recKey);
            } else {
              var args = call["args"];
              if ( promiseCB is Completer ) {
                if (args[1] == null)
                  promiseCB.complete(args[0]);
                else
                  promiseCB.completeError(args[1]);
                if (call["isContinue"] == 0)
                  cbMap.remove(recKey);
              } else {
                promiseCB(args[0],args[1]);
                if ( args[1] != 'CNT' )
                  cbMap.remove(recKey);
              }
            }
          }
        }
      }
    }
  }

  sendReqLoop() async {
    while( outqueue.length > 0 ) {
      var calls = {
        "styp": "array",
        "seq": [ outqueue.length + 1, ...outqueue, 1 /* sequence ?*/
        ]
      };
      var body = jsonEncode(calls);
      var oldQ = outqueue;
      outqueue = [];
      //print("send "+body);
      var response = await http.post(url, headers: { "sid": sid}, body: body);
      if (response.statusCode != 200)
        outqueue = oldQ..addAll(outqueue);
    }
  }

  Future ask( String method, List args ) {
    return _ask(method,args,actorId);
  }

  Future _ask( String method, List args, int actorId ) {
    var cbid = cbidCount++;
    var cb = null;
    if ( args.length > 0 && args[args.length-1] is Function) {
      cbMap[cbidCount] = args[args.length-1];
      args[args.length-1] = null;
      cb = {"typ":"cbw", "obj": [ cbidCount++ ]};
    }
    var request = {
      "typ": 'call',
      "obj": {
        "futureKey": cbid, "queue": 0, "method": method, "receiverKey": actorId,
        "serializedArgs": jsonEncode({
          "styp": "array",
          "seq" : [ args.length, ...args],
        }),
        if ( cb != null ) "cb":cb,
      }
    };
    var completer = new Completer();
    cbMap[cbid] = completer;
    outqueue.add(request);
    sendReqLoop();
    return completer.future;
  }

  void tell( String method, List args ) {
    _tell(method, args, actorId);
  }

  void _tell( String method, List args, int actorId ) {
    var cb = null;
    if ( args.length > 0 && args[args.length-1] is Function) {
      cbMap[cbidCount] = args[args.length-1];
      args[args.length-1] = null;
      cb = {"typ":"cbw", "obj": [ cbidCount++ ]};
    }
    var request = {
      "typ": 'call',
      "obj": {
        "futureKey": 0, "queue": 0, "method": method, "receiverKey": actorId,
        "serializedArgs": jsonEncode({
          "styp": "array",
          "seq" : [ args.length, ...args],
        }),
        if ( cb != null ) "cb":cb,
      }
    };
    outqueue.add(request);
    sendReqLoop();
  }

  dynamic transformJavaJson(inp) {
    if ( inp is Map ) {
      var obj = inp["obj"];
      var typ = inp["typ"];
      if (inp["styp"] != null) {
        var arr = inp["seq"];
        var res = new List();
        for (var i = 1; i < arr.length; ++i) {
          res.add(transformJavaJson(arr[i]));
        }
        return res;
      } else if ( obj is Map) {
        var res = {};
        try {
          obj.forEach((k, v) {
            var transformKJson = transformJavaJson(v);
            res[k] = transformKJson;
          });
          res["_typ"] = typ;
        } catch (e,t) {
          print("$e $t");
        }
        return res;
      } else if ( obj is List) {
        if ( typ.endsWith("_ActorProxy") ) {
          return new RemoteActor(this, inp);
        } else if ( typ == "map") {
          int len = obj[0];
          var res = {};
          for (var i = 1; i < obj.length; i+=2) {
            var k = obj[i];
            var v = transformJavaJson(obj[i+1]);
            res[k] = v;
          }
          return res;
        } else {
          print("unknwon object structure $inp");
        }
      } else
        print("unknwon object structure $inp");
    }
    return inp;
  }

  dynamic toJavaObject( String type, Map obj ) {
    var nobj = {};
    var res = { "typ":type, "obj": nobj };
    obj.forEach( (k,v) {
      if ( v is List ) {
        nobj[k] = toJavaArray(v);
      } else if ( v is Map ) {
        nobj[k] = toJavaMap(v);
      } else
        nobj[k] = v;
    });
    return res;
  }

  toJavaArray(List v) {
    return { "styp":"array", "seq": [v.length,...v] };
  }

  toJavaMap(Map v) {
    var seq = [v.length];
    v.forEach( (k,v) {
      seq.add(k);
      seq.add(v);
    });
    return { "styp":"array", "seq": seq };
  }

}

class RemoteActor {

  int proxyId;
  String clazz;
  KontraktorConnection con;

  RemoteActor(KontraktorConnection con, Map preProcessedRes) {
    proxyId = preProcessedRes["obj"][0];
    clazz = preProcessedRes["obj"][1];
    this.con = con;
  }

  Future ask( String method, List args ) => con._ask(method,args,proxyId);
  void tell( String method, List args ) => con._tell(method,args,proxyId);

}

class RLTable {

  RemoteActor session;
  String table;

  RLTable(session,table) {
    this.session = session;
    this.table = table;
  }

  Future<Map> update( Map jsonObject ) {
    return session.ask("update", [table,jsonEncode(jsonObject)]);
  }

  updateSilent( Map jsonObject ) {
    session.tell("updateAsync",[table,jsonEncode(jsonObject)]);
  }

  Future<bool> delete(key) {
    return session.ask("delete",[this.table,key]);
  }

  deleteSilent(key) {
    this.session.tell("deleteAsync",[table,key]);
  }

  Future<List>fields() async {
    var res = await session.ask("fieldsOf", [table]);
    return res;
  }

  selectAsync(String query, Function(dynamic,dynamic) cb) {
    session.tell("select", [table, query, (r,e) {
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
    session.tell("subscribe", [id, this.table, query, (r,e) {
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
  Future<List> subscribeSyncing(String table, int timeStamp, String query, Function(dynamic,dynamic) cb) async {
    String subsid = uuid.v4();
    int timestamp = await session.ask("subscribeSyncing", [subsid, this.table, timeStamp, query, (r,e) {
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

}

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
    return RLTable(session,name);
  }

}
