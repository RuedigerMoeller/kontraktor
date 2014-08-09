package org.nustaq.kontraktor.remoting.tcp;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.util.RateMeasure;

import java.util.Date;

/**
 * Created by ruedi on 08.08.14.
 */
public class ServerTestFacade extends Actor<ServerTestFacade> {

    public void $testCall( String arg, ClientSideActor actorRef ) {
        System.out.println("received testcall");
        actorRef.$alsoHello("XX "+arg, 113);
    }

    public void $testCallWithCB( long time, Callback<String> cb ) {
        cb.receiveResult(new Date(time).toString(),null);
    }

    RateMeasure measure = new RateMeasure("calls",1000);
    public void $benchMark(int someVal, String someString) {
        measure.count();
    }

}
