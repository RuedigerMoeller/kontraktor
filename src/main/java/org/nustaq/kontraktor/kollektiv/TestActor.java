package org.nustaq.kontraktor.kollektiv;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;

/**
 * Created by ruedi on 07/03/15.
 */
public class TestActor extends Actor<TestActor> {

    public void $init() {
        System.out.println("init");
    }

    public Future<String> $method(String s) {
        System.out.println("method received "+s);
        return new Promise<>(s+s);
    }

}
