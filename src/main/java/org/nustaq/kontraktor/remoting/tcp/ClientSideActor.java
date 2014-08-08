package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.Actor;

/**
 * Created by ruedi on 09.08.14.
 */
public class ClientSideActor extends Actor<ClientSideActor> {

    public void $alsoHello( String x, int y ) {
        System.out.println("x:"+x+" y:"+y);
    }

}
