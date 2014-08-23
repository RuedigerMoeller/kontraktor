package org.nustaq.kontraktor.examples.statefulserver;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.tcp.TCPActorClient;

import java.io.IOException;

/**
 * Created by ruedi on 23.08.2014.
 */
public class StatefulClient extends Actor<StatefulClient> {

    ServerSession session; // this is a remote ref !

    /**
     * @return Boolean.True if successfull, null + error otherwise
     */
    public Future $init() {
        Promise result = new Promise();
        try {
            // connect, if this is successful authenticate to obtain a remote client session object
            TCPActorClient.Connect(StatefulServer.class,"localhost", 6666).then( (server,conError) -> {
                if ( server != null ) {
                    server.$authenticate("user" + Math.random(), "pwd").then((sess, authError) -> {
                        if (sess != null) {
                            session = sess;
                            result.receiveResult(true, null);
                            self().$pingLoop(); // start pinging
                            self().$workLoop(); // simulate work
                        } else {
                            result.receiveResult(null, authError);
                        }
                    });
                } else {
                    result.receiveResult(null,conError);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            result.receiveResult(null,e);
        }
        return result;
    }

    public void $pingLoop() {
        session.$ping();
        delayed(3000, () -> {
            if (!isStopped())
                self().$pingLoop();
        });
    }

    public void $workLoop() {
        // simulate some calls made from client to server
        yield( session.$getUser(), session.$dummyWork() ).then( (futures,error) -> {
            System.out.println( "User:"+futures[0].getResult() );
            System.out.println( "Map:"+futures[1].getResult() );
        });
        delayed(500, () -> {
            if (!isStopped())
                self().$workLoop();
        });
    }

    public static void main( String arg[] ) {
        StatefulClient client = Actors.AsActor(StatefulClient.class);
        client.$init().then( (r,e) -> {
            if ( r == null ) {
                System.out.println("connection error "+e);
                System.exit(-1);
            } else {
                System.out.println("connected and logged in");
            }
        });
    }

}
