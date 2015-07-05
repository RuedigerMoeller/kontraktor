package rxstreamserver;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;

import java.util.Date;

/**
 * Created by ruedi on 05/07/15.
 */
public class RxClient {

    public static void main(String[] args) {
        // FIXME: cancel test. No error on connection close delivered.

        RxStreamServer remoteRef = (RxStreamServer) new TCPConnectable(RxStreamServer.class, "localhost", 7890).connect().await();

        remoteRef.createStream("NUMBERS",0,100)
            .then( (rxPub, errMsg) -> {
                if (rxPub == null) {
                    System.out.println("no stream available:"+errMsg);
                    return;
                }
                rxPub.subscribe( (res,err) -> {
                    if ( Actors.isError(err) ) {
                        System.out.println("error:"+err);
                    } else if ( Actors.isComplete(err)) {
                        System.out.println("complete");
                    } else {
                        System.out.println("received:"+res);
                    }
                });
            });

        remoteRef.listen("TIME")
            .then((rxPub, errMsg) -> {
                if (rxPub == null) {
                    System.out.println("no stream available:"+errMsg);
                    return;
                }
                rxPub.subscribe((time, err) -> {
                    if (Actors.isError(err)) {
                        System.out.println("error:" + err);
                    } else if (Actors.isComplete(err)) {
                        System.out.println("complete. connection closed ?"); // should not happen as infinite stream
                    } else {
                        System.out.println("received:" + new Date((Long) time));
                    }
                });
            });

    }
}
