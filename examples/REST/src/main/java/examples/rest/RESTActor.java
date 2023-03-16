package examples.rest;

import com.eclipsesource.json.JsonObject;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.undertow.util.HeaderMap;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;
import org.nustaq.kontraktor.rest.ContentType;
import org.nustaq.kontraktor.rest.DocHandlerMixin;
import org.nustaq.kontraktor.rest.FromQuery;
import org.nustaq.kontraktor.rest.UndertowRESTHandler;
import org.nustaq.kontraktor.rest.doc.ApiOp;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.Pair;

import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;

@OpenAPIDefinition( info = @Info( title = "Test REST Actor",description = "Some Sample text.") )
public class RESTActor<T extends RESTActor> extends Actor<T> implements DocHandlerMixin {

    // curl -i -X GET http://localhost:8080/api/books/234234
    public IPromise getBooks(
        @FromQuery(value = "id", desc = "the unique id of the book") int id
    ) {
        Promise res = promise();
        // simulate blocking operation (e.g. database query)
        execInThreadPool( () -> new Book().title("Title "+id).id(""+id).author( new Author().firstName("kontraktor") ) ).
            then(res);
        return res;
    }

    //curl -i -X POST --data "param1=value1&param2=value2" http://localhost:8080/api/stuff
    @Operation( summary = "post a raw resource" )
    public IPromise postStuff( byte[] body, @FromQuery(value = "name", desc = "name of the resource") String name ) {
        Log.Info(this,"posted:"+new String(body));
        return resolve(200);
    }

    @Operation(
        summary = "query authors",
        description = "big bla big bla big bla big bla big bla big bla big bla big bla big bla big" +
            " bla big bla big bla big bla big bla big bla big bla big bla big bla big bla big bla ",
        responses = @ApiResponse( content = @Content(array = @ArraySchema( schema = @Schema( implementation = Author.class) ) ) )
    )
    public IPromise<List<Author>> getQueryUsers(
        @FromQuery(value = "firstName", desc = "substring of firstName") String firstName,
        @FromQuery(value = "birthdate", desc = "born before this") long beforeDate )
    {
        return resolve(
            Arrays.asList(
                new Author().firstName("X").lastName("Meier"),
                new Author().firstName("Y").lastName("Müller")
            ));
    }

    @ApiOp(
        summary = "query authors",
        description = "big bla big bla big bla big bla big bla big bla big bla big bla big bla big" +
            " bla big bla big bla big bla big bla big bla big bla big bla big bla big bla big bla ",
        response = Author.class,
        container = "array"
    )
    public IPromise<List<Author>> getQueryUsers1(
        @FromQuery(value = "firstName", desc = "substring of firstName") String firstName,
        @FromQuery(value = "birthdate", desc = "born before this") long beforeDate )
    {
        return resolve(
            Arrays.asList(
                new Author().firstName("X").lastName("Meier"),
                new Author().firstName("Y").lastName("Müller")
            ));
    }

    @Operation(
        summary = "post a new Book",
        description = "Lorem ipsum pok. Lorem ipsum pok. mean perceived probability that " +
            "U.S. stock prices will be higher 12 months from now increased." +
            "U.S. stock prices will be higher 12 months from now increased." +
            "U.S. stock prices will be higher 12 months from now increased.",
            responses = @ApiResponse( content = @Content(array = @ArraySchema( schema = @Schema( implementation = Book.class) ) ) )
        )
    public IPromise postBook( Book body, @FromQuery(value = "parm",desc = "jsut a test variable") boolean test ) {
        Log.Info(this,"posted:"+body.toJsonString());
        return resolve(body);
    }

    @ApiOp(
        summary = "post a new Book",
        description = "Lorem ipsum pok. Lorem ipsum pok. mean perceived probability that " +
            "U.S. stock prices will be higher 12 months from now increased." +
            "U.S. stock prices will be higher 12 months from now increased." +
            "U.S. stock prices will be higher 12 months from now increased.",
        response = Book.class
    )
    public IPromise postBook1( Book body, @FromQuery(value = "parm",desc = "jsut a test variable") boolean test ) {
        Log.Info(this,"posted:"+body.toJsonString());
        return resolve(body);
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
            .restAPI("/", act )
            .build();
    }

    @Override @CallerSideMethod
    public boolean isDocEnabled() {
        return true;
    }
}