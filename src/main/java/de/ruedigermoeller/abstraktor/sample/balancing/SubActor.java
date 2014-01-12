package de.ruedigermoeller.abstraktor.sample.balancing;

import de.ruedigermoeller.abstraktor.Actor;
import de.ruedigermoeller.abstraktor.Future;

/**
 * Created by ruedi on 1/8/14.
 */
public class SubActor extends Actor {

    public void doAlsoWork( SubActor otherOne, Future<String> resultReceiver ) {
        if ( otherOne != null ) {
            otherOne.doAlsoWork( null, resultReceiver );
        }
        resultReceiver.receiveObjectResult("Ok so far");
    }

}
