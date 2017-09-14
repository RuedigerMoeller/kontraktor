package examples.rest;

import com.eclipsesource.json.JsonObject;
import io.undertow.util.HeaderMap;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;
import org.nustaq.kontraktor.rest.ContentType;
import org.nustaq.kontraktor.rest.FromQuery;
import org.nustaq.kontraktor.rest.UndertowRESTHandler;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.Pair;
import java.util.Deque;
import java.util.Map;

public class RESTActor<T extends RESTActor> extends Actor<T> {

    // curl -i -X GET http://localhost:8080/api/books/234234
    public IPromise getBooks( int id ) {
        Promise res = promise();
        // simulate blocking operation (e.g. database query)
        execInThreadPool( () -> new Book().title("Title "+id).id(""+id).author("kontraktor") ).
            then(res);
        return res;
    }

    //curl -i -X POST --data "param1=value1&param2=value2" http://localhost:8080/api/stuff
    public IPromise postStuff( byte[] body ) {
        Log.Info(this,"posted:"+new String(body));
        return resolve(200);
    }

    //curl -i -X POST --data "{ \"key\": \"value\", \"nkey\": 13 }" http://localhost:8080/api/stuff1
    public IPromise postStuff1(JsonObject body, HeaderMap headerValues ) {
        headerValues.forEach( hv -> {
            Log.Info(this,""+hv.getHeaderName());
            hv.forEach( s -> {
                Log.Info(this,"    "+s);
            });
        });
        Log.Info(this,""+body);
        return resolve(new Pair(202,body.toString()));
    }

    //curl -i -X PUT --data "{ \"name\": \"satoshi\", \"nkey\": 13345 }" 'http://localhost:8080/api/user/nakamoto/?x=simple&something=pokpokpok&x=13&age=111'
    @ContentType("application/json")
    public IPromise putUser(String name, @FromQuery("age") int age, JsonObject body, Map<String,Deque<String>> queryparms) {
        Log.Info(this,"name:"+name+" age:"+age);
        queryparms.forEach( (k,v) -> {
            Log.Info(this,""+k+"=>");
            v.forEach( s -> {
                Log.Info(this,"    "+s);
            });
        });
        return resolve(body);
    }

    public static void main(String[] args) {
        RESTActor act = AsActor(RESTActor.class);
        Http4K.Build("localhost", 8080)
            .restAPI("/api", act )
            .build();
    }

}