package org.nustaq.kontraktor;

import org.nustaq.kontraktor.remoting.http.Http4K;
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

    public static void main(String a) {
        HelloActor actor = AsActor(HelloActor.class);
        Http4K.get().publish( new WebSocketPublisher( actor, "localhost", "/hello", 8080 ) );
    }

}