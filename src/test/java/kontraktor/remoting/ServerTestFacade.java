package kontraktor.remoting;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.tcp.TCPActorServer;
import org.nustaq.kontraktor.util.RateMeasure;

import java.io.IOException;
import java.util.Date;

/**
 * Created by ruedi on 08.08.14.
 */
public class ServerTestFacade extends Actor<ServerTestFacade> {

    public void $testCall( String arg, ClientSideActor actorRef ) {
        System.out.println("received testcall");
        actorRef.$alsoHello("XX "+arg, 113);
    }

    public void $testCallWithCB( long time, Callback<String> cb ) {
        cb.receive(new Date(time).toString(), null);
    }

    RateMeasure measure = new RateMeasure("calls",1000);
    public void $benchMark(int someVal, String someString) {
        measure.count();
    }

    public Future<String> $doubleMe( String s ) {
        return new Promise<>(s+" "+s);
    }


    public static void main(String arg[]) throws Exception {
        TCPActorServer.Publish(Actors.AsActor(ServerTestFacade.class), 7777);
    }

}
