package kontraktor.remoting.helpers;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.tcp.TCPActorServerAdapter;
import org.nustaq.kontraktor.remoting.websocket.WebSocketActorServerAdapter;
import org.nustaq.kontraktor.util.RateMeasure;

import java.util.Date;

/**
 * Created by ruedi on 08.08.14.
 */
public class ServerTestFacade extends Actor<ServerTestFacade> {


    public void $testCall( String arg, ClientSideActor actorRef ) {
        System.out.println("received testcall");
        actorRef.$alsoHello("XX "+arg, 113);
    }

    public IPromise<String> $futureTest(String s) {
        return new Promise<>(s+" "+s);
    }

    public void $testCallWithCB( long time, Callback<String> cb ) {
        cb.complete(new Date(time).toString(), null);
    }

    public void $sporeTest( Spore<Integer,Integer> spore ) {
        spore.remote(1);
        spore.remote(2);
        spore.remote(3);
        spore.remote(4);
        spore.finish();
    }

    RateMeasure measure = new RateMeasure("calls",1000);
    public void $benchMark(int someVal, String someString) {
        measure.count();
    }

    public IPromise $benchMark1(int someVal, String someString) {
        measure.count();
        return new Promise<>("ok");
    }

    public static TCPActorServerAdapter run() throws Exception {
        return TCPActorServerAdapter.Publish(Actors.AsActor(ServerTestFacade.class), 7777);
    }

    public static WebSocketActorServerAdapter runWS() throws Exception {
        return WSServer.run();
    }

}
