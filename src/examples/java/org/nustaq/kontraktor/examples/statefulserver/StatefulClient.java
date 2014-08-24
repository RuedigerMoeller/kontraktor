package org.nustaq.kontraktor.examples.statefulserver;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.tcp.TCPActorClient;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by ruedi on 23.08.2014.
 */
public class StatefulClient extends Actor<StatefulClient> {

    StatefulServer server;
    ServerSession session; // this is a remote ref !
    boolean stopPing; // testing

    /**
     * @return Boolean.True if successfull, null + error otherwise
     */
    public Future $init() {
        Promise result = new Promise();
        try {
            // connect, if this is successful authenticate to obtain a remote client session object
            TCPActorClient.Connect(StatefulServer.class,"localhost", 6666).then( (server,conError) -> {
                if ( server != null ) {
                    StatefulClient.this.server = server;
                    server.$authenticate("user" + Math.random(), "pwd").then((sess, authError) -> {
                        if (sess != null) {
                            session = sess;
                            result.receiveResult(true, null);
                            self().$pingLoop(); // start pinging
                            self().$workLoop(); // simulate work
                        } else {
                            result.receiveResult(null, authError);
                            server.$close();
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
            if (!isStopped() && ! stopPing )
                self().$pingLoop();
        });
    }

    public void $testStopPing() {
        stopPing = true; // expect server to close connection
    }

    @Override
    public void $stop() {
        session.$stop();
        server.stopSafeClose();
        super.$stop();
    }

    public void $workLoop() {
        if ( session.isStopped() || server.isStopped() ) {
            $stop();
            System.out.println("session stopped:"+session.isStopped());
            System.out.println("server stopped:"+server.isStopped());
            return;
        }
        // simulate some calls made from client to server
        yield( session.$getUser(), session.$dummyWork() ).then((futures, error) -> {
            System.out.println("User:" + futures[0].getResult());
            System.out.println("Map:" + futures[1].getResult());
        });
        delayed(500, () -> {
            if (!isStopped())
                self().$workLoop();
        });
    }

    /////////////////////// end of client code, some static setup + test methods ////////////////////////////////////////////

    public static StatefulClient createClient() {
        StatefulClient client = Actors.AsActor(StatefulClient.class);
        client.$init().then( (r,e) -> {
            if ( r == null ) {
                System.out.println("connection error "+e);
                System.exit(-1);
            } else {
                System.out.println("connected and logged in");
            }
        });
        return client;
    }

    public static void testConnectDisconnect() throws InterruptedException {
        while(true) {
            ArrayList<StatefulClient> clients = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                clients.add(createClient());
            }
            Thread.sleep(5000);
            clients.forEach((cl) -> cl.$stop());
            Thread.sleep(5000);
        }
    }

    public static void testStopPing() throws InterruptedException {
        StatefulClient client = createClient();
        Thread.sleep(5000);
        client.$testStopPing();
        while( ! client.isStopped() ) {
            Thread.sleep(1000);
            System.out.println("wait for stop ..");
        }
    }

    public static void main( String arg[] ) throws InterruptedException {
        //createClient();
        //testConnectDisconnect();
        testStopPing();
    }


}
