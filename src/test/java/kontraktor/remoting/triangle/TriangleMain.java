package kontraktor.remoting.triangle;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.remoting.tcp.TCPActorClient;
import org.nustaq.kontraktor.remoting.tcp.TCPActorServer;

import java.io.IOException;

/**
 * Created by ruedi on 13.08.2014.
 */
public class TriangleMain {

    public static void main(String arg[]) throws Exception {
        final TCPActorServer server = TCPActorServer.Publish(Actors.AsActor(CenterActor.class), 7777);

        Future<CenterActor> outer1 = TCPActorClient.Connect(CenterActor.class, "localhost", 7777);
        Future<CenterActor> outer2 = TCPActorClient.Connect(CenterActor.class, "localhost", 7777);
        Future<CenterActor> outer3 = TCPActorClient.Connect(CenterActor.class, "localhost", 7777);

        // wait til all have connected
        Actors.yield(outer1,outer2,outer3).then( (futures,error) -> {
            // create client actors
            OuterActor outer[] = {
                    (OuterActor) Actors.AsActor(OuterActor.class),
                    (OuterActor) Actors.AsActor(OuterActor.class),
                    (OuterActor) Actors.AsActor(OuterActor.class)
            };
            // register each client actor
            for (int i = 0; i < futures.length; i++) {
                CenterActor center = (CenterActor) futures[i].getResult();
                center.$registerRemoteRef(i, outer[i]);
                outer[i].$init(i, center);
            }

            // chained remoterefs:
            // outer 0 looks up a remoteref using its connection to center and does a
            // call on that. chain is
            //   invoke $sendCall[local] to outer[0]
            //   outer[0].sendCall does a remote call to center actor retrieving a remote ref to outer[1]
            //   sendCall makes a remoteCall on the outer[1] remote ref. this is sent to center, as center is the owner of the remoteref
            //   center forwards the call to its remoteref of outer[1]
            //   outer[1] receives the call and async sends the result via center=>outer[0]
            outer[0].$sendCall(1,"Hello from 0").then( (r,e) -> {
                System.out.println("received '"+r+"'");
                for (int i = 0; i < outer.length; i++) {
                    OuterActor outerActor = outer[i];
                    outerActor.$stop();
                }

                // note: theoretically any client can stop server actor by calling $stop ..
                // server.stop();FIXME
            });
        });
    }

}
