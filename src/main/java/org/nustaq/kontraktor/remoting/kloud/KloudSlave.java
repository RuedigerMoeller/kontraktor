package org.nustaq.kontraktor.remoting.kloud;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.tcp.TCPActorClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by moelrue on 3/6/15.
 */
public class KloudSlave extends Actor<KloudSlave> {

    String tmpDir = "./tmp";
    List<String> masterAddresses;
    HashMap<String,KloudMaster> masters;
    String nodeId = "SL"+System.currentTimeMillis()+":"+(System.nanoTime()&0xffff);

    public void $init() {
        List<String> addr = new ArrayList<>();
        addr.add("127.0.0.1:3456");
        $initWithOptions(addr);
    }

    public void $initWithOptions(List<String> masterAddresses) {
        this.masterAddresses = masterAddresses;
        new File(tmpDir).mkdirs();
        masters = new HashMap<>();
        $startHB();
    }

    public void $startHB() {
        checkThread();
        for (int i = 0; i < masterAddresses.size(); i++) {
            String s = masterAddresses.get(i);
            if ( masters.get(s) == null ) {
                String[] split = s.split(":");
                try {
                    TCPActorClient.Connect(
                        KloudMaster.class,
                        split[0], Integer.parseInt(split[1]),
                        disconnectedRef -> self().$refDisconnected(s,disconnectedRef)
                    )
                    .onResult(actor -> {
                        masters.put(s, actor);
                        actor.$registerSlave(new SlaveDescription(nodeId), self());
                    })
                    .onError(err -> System.out.println("failed to connect " + s));
                } catch (IOException e) {
                    System.out.println("could not connect "+e);
                }
            }
        }
        delayed(3000, () -> self().$startHB());
    }

    public void $refDisconnected(String address, Actor disconnectedRef) {
        checkThread();
        System.out.println("actor disconnected "+disconnectedRef+" address:"+address);
        masters.remove(address);
    }

    public Future<Actor> $start( String clazzname, String nameSpace ) {
        return new Promise<>(null);
    }

    public Future $defineNameSpace( ActorAppBundle bundle ) {
        System.out.println("define name space "+bundle.getName());
        return new Promise<>(null);
    }

    public static void main( String a[] ) {
        KloudSlave sl = Actors.AsActor(KloudSlave.class);
        sl.$init();
        sl.$refDisconnected(null,null);
    }

}
