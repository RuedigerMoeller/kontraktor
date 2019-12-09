import 'dart:io';
import 'dart:convert';

class ReflectionReplacementTest {

  int instanceVar = 13;
  Map<String,Function> public;

  ReflectionReplacementTest() {
    public = {
      "login": (String a, String b) async => login( a, b),
    };
  }
  Future login( String a, String b) async {
    return a+b;
  }
}


main() {
  HttpServer
      .bind(InternetAddress.anyIPv6, 8087)
      .then((server) {
    server.listen((HttpRequest request) async {
      if ( request.method == "POST" ) {
        String postBody = await request.cast<List<int>>().transform(Utf8Decoder(allowMalformed:true)).join();
        print( postBody );
        if ( postBody.length > 0 )
          print( jsonDecode(postBody) );
      }
      request.response.write('Hello, world!');
      request.response.close();
    });
  });
  print ("done");
}
