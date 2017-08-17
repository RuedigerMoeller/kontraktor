package org.nustaq.kontraktor.examples;

import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.HttpConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;

import static java.util.Arrays.*;

/**
 * Created by ruedi on 18/06/15.
 */
public class HelloClient {


    public static void main( String args[] ) {

        org.nustaq.kontraktor.remoting.base.ConnectableActor connectables[] = {
            new WebSocketConnectable(HelloActor.class,"http://localhost:8080/hello")
                .serType(SerializerType.FSTSer),
            new HttpConnectable( HelloActor.class, "http://localhost:8080/hellohttp" )
                .serType(SerializerType.JsonNoRefPretty),
            new TCPConnectable( HelloActor.class, "localhost", 6789 )
        };

        stream(connectables).forEach( connectable -> {
            HelloActor remote = (HelloActor)
                connectable
                    .connect((res, err) -> System.out.println(connectable + " disconnected !"))
                    .await();
            remote.getMyName().then( name -> {
                System.out.println(connectable.getClass().getSimpleName()+" "+name);
            });
        });
    }

}
