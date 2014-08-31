package websockets;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.http.netty.wsocket.WSocketActorClient;
import org.nustaq.kontraktor.remoting.http.netty.wsocket.WSocketActorServer;
import org.nustaq.netty2go.NettyWSHttpServer;

import java.io.File;

/**
 * Created by ruedi on 31.08.14.
 */
public class WSTestActor extends Actor<WSTestActor> {

    public void $voidCall(String message) {
        System.out.println("receive msg:"+message);
    }

    public Future $futureCall(String result) {
        return new Promise<>(result);
    }

    public void $callbackTest(String msg, Callback cb) {
        cb.receive(msg,null);
    }


    public static void main(String arg[]) throws Exception {
        if ( arg.length == 0 ) {
            startServer();
        } else {
            startClient();
        }
    }

    private static void startClient() throws Exception {
        WSocketActorClient cl = new WSocketActorClient(WSTestActor.class,"ws://localhost:8887/websocket");
        cl.connect();
        WSTestActor facadeProxy = (WSTestActor) cl.getFacadeProxy();
        while( true ) {
            facadeProxy.$voidCall("Hello");
            Thread.sleep(1000);
        }
    }

    private static void startServer() throws Exception {
        Actor actor = Actors.AsActor(WSTestActor.class);
        WSocketActorServer server = new WSocketActorServer( actor, new File("./") );
        new NettyWSHttpServer(8887, server).run();
    }
}
