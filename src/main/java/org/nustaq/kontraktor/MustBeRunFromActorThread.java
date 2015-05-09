package org.nustaq.kontraktor;

/**
 * Created by ruedi on 09/05/15.
 */
public class MustBeRunFromActorThread extends RuntimeException {

    public MustBeRunFromActorThread() {
        super("expects to run in actor thread");
    }
}
