package de.ruedigermoeller.abstractor.sample.balancing;

import de.ruedigermoeller.abstractor.Actor;
import de.ruedigermoeller.abstractor.Future;

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
