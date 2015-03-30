package uttest;

import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Spore;
import org.nustaq.kontraktor.remoting.websocket.WebSocketClient;

/**
 * Created by ruedi on 28/03/15.
 */
public class WSClient {

    public static void main(String[] args) {
        String destUri = "ws://localhost:8080/ws";
        if (args.length > 0) {
            destUri = args[0];
        }
        try {
            Future<WSTestActor> connect = WebSocketClient.Connect(WSTestActor.class, "ws://localhost:8080/ws", null);
            WSTestActor actor = connect.await();

            actor.$voidMethod();
            actor.$futureMethod().then( r -> System.out.println("future returned "+r) );
            actor.$streamCallback( (r,e) -> {
                if ( actor.isResult(e) )
                    System.out.println("streamed "+r+" "+e);
                else if ( actor.isFinal(e) ) {
                    System.out.println("done");
                } else {
                    System.out.println("error");
                }
            });

            actor.$sporeTest(
                new Spore<String, String>() {
                    @Override
                    public void remote(String input) {
                        stream(input+"::"+input);
                    }
                }
                .forEach((res, err) -> System.out.println(res))
                .onFinish(() -> System.out.println("spore done"))
            );

            Object awaitRes = actor.$futureMethod().await();
            System.out.println("awaitres "+awaitRes);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
