import 'dart:io';
import 'dart:convert';
import 'package:uuid/uuid.dart';

class ReflectionReplacementTest {

  int instanceVar = 13;
  Map<String,Function> public;

  ReflectionReplacementTest() {
    public = {
      "login": (String a, String b) => login( a, b),
    };
  }
  Future login( String a, String b) async {
    return a+b;
  }
}

var uuid = Uuid();

main() {
  HttpServer
      .bind(InternetAddress.anyIPv6, 8087)
      .then((server) {
    server.listen((HttpRequest request) async {
      if ( request.method == "POST" ) {
        String postBody = await request.cast<List<int>>().transform(Utf8Decoder(allowMalformed:true)).join();
        print( "BODY:" + postBody );
        if ( postBody.length > 0 ) {
          print("sid:" + request.headers.value("sid"));
          print(jsonDecode(postBody));
        }
        else {
          // create session id
          // TODO: generate connection object
          request.response.write('"'+uuid.v4()+'"');
          request.response.close();
        }
      } else {
        request.response.write('Hello, world!');
        request.response.close();
      }
    });
  });
  print ("done");
}
