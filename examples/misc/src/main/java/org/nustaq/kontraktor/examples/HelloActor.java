package org.nustaq.kontraktor.examples;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.Http4K;
import org.nustaq.kontraktor.remoting.http.HttpPublisher;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.remoting.websockets.WebSocketPublisher;

/**
 * Created by ruedi on 07/06/15.
 */
public class HelloActor extends Actor<HelloActor> {

    String myName = "Kevin";

    public void setMyName( String name ) {
        if ( ! "Rüdiger".equals(name) )
            myName = name;
    }

    public IPromise<String> getMyName() {
        return new Promise<>(myName);
    }

    public void streamMyName( Callback<Character> channel ) {
        myName.chars().forEach( ch -> channel.stream( (char) ch) );
        channel.finish();
    }

    public static void main(String a[]) {

        HelloActor myService = AsActor(HelloActor.class);

        // as websocket service fast serialialized
        new WebSocketPublisher()
            .facade(myService)
            .hostName("localhost")
            .urlPath("/hello")
            .port(8080)
            .serType(SerializerType.FSTSer)
            .publish();

        // as http long poll service, json encoding
        new HttpPublisher()
            .facade(myService)
            .hostName("localhost")
            .urlPath("/hellohttp")
            .port(8080)
            .serType(SerializerType.JsonNoRefPretty)
            .publish();

        // as tcp nio service, fast serialized
        new TCPNIOPublisher()
            .facade(myService)
            .port(6789)
            .serType(SerializerType.FSTSer)
            .publish().await();

    }

}