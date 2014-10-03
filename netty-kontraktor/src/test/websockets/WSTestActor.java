package websockets;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.http.netty.util.ActorWSServer;
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
        System.out.println("futcall "+result );
        return new Promise<>(result);
    }

    public void $callbackTest(String msg, Callback cb) {
        System.out.println("cb call "+msg);
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
        WSocketActorClient cl = new WSocketActorClient(WSTestActor.class,"ws://localhost:8887/websocket", ActorWSServer.Coding.MinBin);
        cl.connect();
        WSTestActor facadeProxy = (WSTestActor) cl.getFacadeProxy();

        int count =0;
        while( count++ < 5 ) {
            facadeProxy.$voidCall("Hello void");
            Thread.sleep(1000);
            facadeProxy.$futureCall("hello future").then((r, e) -> System.out.println("future call worked result:" + r));
            Thread.sleep(1000);
            facadeProxy.$callbackTest("hello cb", (r,e) -> System.out.println("cb result "+r) );
            Thread.sleep(1000);
        }

        facadeProxy.$close();
    }

    private static void startServer() throws Exception {
        Actor actor = Actors.AsActor(WSTestActor.class);
        WSocketActorServer server = new WSocketActorServer( actor, new File("./"), ActorWSServer.Coding.MinBin );
	    server.setFileMapper( (f) -> {
		    if ( f != null && f.getName() != null ) {
			    if ( f.getName().equals("minbin.js") ) {
				    return new File("/home/ruedi/IdeaProjects/fast-serialization/src/main/javascript/minbin.js");
			    }
		    }
		    return f;
	    });
        new NettyWSHttpServer(8887, server).run();
    }
}
