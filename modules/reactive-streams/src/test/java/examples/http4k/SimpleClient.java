package examples.http4k;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.reactivestreams.KxReactiveStreams;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;

/**
 * Created by ruedi on 11/07/15.
 */
public class SimpleClient {

    public static void main(String[] args) {

        KxReactiveStreams.get()
            .connect( Long.class, new WebSocketConnectable().url("ws://localhost:8080/ws/seconds") )
            .async()
            .subscribe( (time,err) -> {
                if ( Actors.isResult(err) ) {
                    System.out.println("seconds:"+time);
                } else {
                    System.out.println("stream 'seconds' has closed");
                }
            });

        KxReactiveStreams.get()
            .connect( Long.class, new WebSocketConnectable().url("ws://localhost:8080/ws/minutes") )
            .async()
            .subscribe( (time,err) -> {
                if ( Actors.isResult(err) ) {
                    System.out.println("MINUTE:"+time);
                } else {
                    System.out.println("stream 'ms' has closed");
                }
            });

    }

}
