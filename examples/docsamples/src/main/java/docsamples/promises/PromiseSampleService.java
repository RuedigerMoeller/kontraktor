package docsamples.promises;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.WebSocketPublisher;

import java.net.URL;
import java.util.Scanner;

public class PromiseSampleService<T extends PromiseSampleService> extends Actor<T> {

    ////////////////////////////////////////////////////
    // these methods are identical ...

    public IPromise<String> getDataSimple() {
        Promise result = new Promise();
        result.complete("Data", null);
        return result;
    }

    public IPromise<String> getDataSimple1() {
        Promise result = new Promise();
        result.resolve("Data");
        return result;
    }

    public IPromise<String> getDataSimple2() {
        return new Promise("Data");
    }

    public IPromise<String> getDataSimple3() {
        return resolve("Data");
    }

    ////////////////////////////////////////////////////


    public IPromise<String> getDataAsync() {
        Promise p = new Promise();
        // simulate async long running op
        delayed(3000,() -> p.resolve("Data"));
        return p; // returns before result is present
    }

    public IPromise<String> getURLContent(final String url ) {
        Promise<String> prom = new Promise(); // unresolved promise
        // as URL.openStream is blocking API, run this external on a thread pool
        exec(
            () -> new Scanner(new URL(url).openStream(), "UTF-8").useDelimiter("\\A").next()
        ).then( (result, error) ->
            prom.complete(result, error) // runs in actor thread again (*)
        );
        // caveat: execute() executes on the actor thread, exec() on a threadpool
        return prom;
    }

    public IPromise<String> getURLContent1(final String url ) {
        Promise<String> prom = new Promise();
        exec(
            () -> new Scanner(new URL(url).openStream(), "UTF-8").useDelimiter("\\A").next()
        ).then( prom ); // <= shorthand for code marked (*) above
        return prom;
    }

    public static void main(String[] args) {
        PromiseSampleService facade = AsActor(PromiseSampleService.class);
        new WebSocketPublisher()
            .hostName("localhost")
            .port(9084)
            .urlPath("/endpoint")
            .serType(SerializerType.FSTSer)
            .facade(facade)
            .publish(x-> System.out.println("disconnected "+x));
    }
}
