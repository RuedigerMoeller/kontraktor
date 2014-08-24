package org.nustaq.kontraktor.examples.statefulserver;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.impl.ElasticScheduler;
import org.nustaq.kontraktor.remoting.tcp.TCPActorServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ruedi on 23.08.2014.
 *
 * Example of an actor based stateful server via TCP. The facade actor (=StateFulServer) creates a ServerSession
 * actor per client (after authentication).
 * Clients obtain a remote ref (transparent) of their designated ServerSession object. Note that ServerSessions
 * share a common scheduler in order to avoid creating a thread for each client.
 *
 * Note this cannot be exported as a WebService currently as Http remoting does not support remote actor references,
 * so only stateless services (actors) can be exported.
 */
public class StatefulServer extends Actor<StatefulServer> {

    List<ServerSession> sessions;
    ElasticScheduler clientScheduler;

    public void $init( int numClientThreads ) {
        sessions = new ArrayList<>();
        clientScheduler = new ElasticScheduler(numClientThreads,5000);
    }

    public Future<ServerSession> $authenticate( String user, String pwd ) {
        if ( user != null && pwd != null ) // dummy auth
        {
            ServerSession newSession = Actors.AsActor(ServerSession.class,clientScheduler);
            newSession.$init( user, self() );
            sessions.add(newSession);
            System.out.println("added session "+user);
            return new Promise<>(newSession,null);
        }
        return new Promise<>(null,"authentication failure");
    }

    public void $clientTerminated(ServerSession session) {
        sessions.remove(session);
    }

    public static void main( String arg[] ) throws IOException {
        StatefulServer act = Actors.AsActor(StatefulServer.class);
        act.$init(4);

        TCPActorServer.Publish(act, 6666);
    }
}
