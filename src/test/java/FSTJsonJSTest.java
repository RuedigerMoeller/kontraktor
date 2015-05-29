import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.fourk.Http4K;
import org.nustaq.kontraktor.remoting.http.HttpClientConnector;
import org.nustaq.kontraktor.remoting.http.UndertowHttpServerConnector;

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
        UndertowHttpServerConnector.Publish(jsa,"localhost","/jsactor/",8080,new Coding(SerializerType.JsonNoRefPretty));

        JSActor client = HttpClientConnector.Connect(JSActor.class, "http://localhost:8080/jsactor", null, null, new Coding(SerializerType.JsonNoRefPretty)).await();
        System.out.println(client.say("blabla").await());

    }

}
