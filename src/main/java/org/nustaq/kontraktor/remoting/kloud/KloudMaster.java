package org.nustaq.kontraktor.remoting.kloud;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.remoting.tcp.TCPActorServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by moelrue on 3/6/15.
 */
public class KloudMaster extends Actor<KloudMaster> {

    List<KloudSlave> slaves;

    public void $init() {
        slaves = new ArrayList<>();
    }

    public void $registerSlave(SlaveDescription sld, KloudSlave slave ) {
        System.out.println("receive registration "+sld);
        slaves.add(slave);
        slave.$defineNameSpace(new ActorAppBundle("Hello"));
    }

    public static void main(String arg[]) throws IOException {
        KloudMaster cm = Actors.AsActor(KloudMaster.class);
        cm.$init();
        TCPActorServer.Publish( cm, 3456 );
    }
}
