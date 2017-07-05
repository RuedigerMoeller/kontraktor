var http = require('http');

var jskXMLHttpRequest = function() {
  var _type, _url, _bool;
  var _headers = {};

  this.open = function( type, url, boolwhatever) {
    _type = type; _url = url; _bool = boolwhatever;
  };
  this.setRequestHeader = function( key, value ) {
    _headers[key] = value;
  };


  this.send = function( data ) {
    var self = this;

    _headers['Content-Length'] = Buffer.byteLength(data);
    var slslidx = _url.indexOf("//");
    var pathidx = _url.indexOf("/",slslidx+2);
    var dom = _url.substring(slslidx+2,pathidx);
    var port = 80;
    var dpidx = dom.indexOf(":");
    if (dpidx > 0) {
      port = dom.substring(dpidx+1);
      dom = dom.substring(0,dpidx);
    }
    var post_options = {
      host: dom,
      port: port,
      path: _url.substring(pathidx),
      method: _type,
      headers: _headers
    };
    var rawData = '';
    var post_req = http.request(post_options, function(res) {
      res.setEncoding('utf8');
      res.on('data', function (chunk) {
        rawData += chunk;
      });
      res.on("end", function () {
        self.readyState = 4;
        self.status = res.statusCode;
        self.statusText = res.statusMessage;
        self.responseText = rawData;
        self.onreadystatechange();
      });
      res.on("error", function (err) {
        self.readyState = 4;
        self.status = res.statusCode;
        self.statusText = res.statusMessage;
        self.responseText = "";
        self.onreadystatechange();
      });
    });
    // post the data
    post_req.write(data);
    post_req.end();
  };
};
jskXMLHttpRequest.DONE = 4;

module.exports = jskXMLHttpRequest;