# Asynchronous REST service

Example on how to map rest requests to actor methods.
Note the RestActor is single threaded so use execInThreadPool to perform long running,
blocking operations.

REST requests are mapped using method name conventions, for query parsing
annotations can be used. In addition its possible to directly receive request body
data by having a byte[] or JsonObject parameter. request header values are injected
into a HeaderMap parameter if present, all query values can be received in a Map<String,Deque<String>
parameter (if present). 

```java
public IPromise [http-method][firstPathElement](
    2cndPathElement, 3rdPathElement, .., 
    @FromQuery("name") String value,
    byte[] rawRequestBody,
    JsonObject parsedRequestBody,
    HeaderMap requestHeader,
    Map<String,Deque<String>> queryParams
)
```

all parameters are optional.
A http response is built from a Promise result. If it is an int => a http response with the
given int is returned (e.g. prom.resolve(404)). If it's a String this is returned directly,
a Pair<Integer,String> will result in a response with status code and given string as a body,
a Serializable Pojo will be transformed to Json (using FST conventions), a JsonValue will
be Stringified.