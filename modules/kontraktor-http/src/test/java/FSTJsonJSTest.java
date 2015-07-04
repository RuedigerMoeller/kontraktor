import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.*;

import java.io.File;

/**
 * Created by ruedi on 25/05/15.
 */
public class FSTJsonJSTest {

    public static class JSActor extends Actor<JSActor> {

        public IPromise<String> say( String what ) {
            return resolve("I say:"+what);
        }

    }

    public static void main( String a[] ) {
        HttpClientConnector.DumpProtocol = true;

        File root = new File("../fast-serialization/src/main/web/");
        Http4K.get().publishFileSystem("localhost","/",8080, root);

        JSActor jsa = Actors.AsActor(JSActor.class);

        new HttpPublisher(jsa,"localhost","/jsactor/",8080)
            .serType(SerializerType.JsonNoRefPretty)
            .publish().await();

        JSActor client = (JSActor)
            new HttpConnectable(JSActor.class, "http://localhost:8080/jsactor")
                .serType(SerializerType.JsonNoRefPretty)
                .connect()
                .await();

        System.out.println(client.say("blabla").await());
    }

}
