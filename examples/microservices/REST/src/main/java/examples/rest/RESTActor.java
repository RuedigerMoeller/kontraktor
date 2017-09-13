package examples.rest;

import com.eclipsesource.json.JsonObject;
import io.undertow.util.HeaderMap;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.Pair;
import java.util.Deque;
import java.util.Map;

public class RESTActor<T extends RESTActor> extends Actor<T> {

    public IPromise getBooks( int id ) {
        return new Promise(new Book().title("Title "+id).id(""+id).author("kontraktor"));
    }

    public IPromise postStuff( byte[] body ) {
        Log.Info(this,"posted:"+new String(body));
        return resolve(200);
    }

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
            .httpHandler("/api", new UndertowRESTHandler("/api", act))
            .build();
    }

}