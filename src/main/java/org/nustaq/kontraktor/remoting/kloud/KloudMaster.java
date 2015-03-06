package org.nustaq.kontraktor.remoting.kloud;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.remoting.tcp.TCPActorServer;

import java.io.IOException;

/**
 * Created by moelrue on 3/6/15.
 */
public class KloudMaster extends Actor<KloudMaster> {

    public void $init() {

    }

    public void $registerSlave(SlaveDescription sld, KloudSlave slave ) {
        System.out.println("receive registration "+sld);
    }

    public static void main(String arg[]) throws IOException {
        KloudMaster cm = Actors.AsActor(KloudMaster.class);
        cm.$init();
        TCPActorServer.Publish( cm, 3456 );
    }
}
