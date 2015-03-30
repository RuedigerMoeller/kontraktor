package uttest;

import org.nustaq.kontraktor.*;

/**
* Created by ruedi on 31.03.2015.
*/
public class WSTestActor extends Actor<WSTestActor> {

    public void $voidMethod() {
        System.out.println("! void !"+Thread.currentThread());
    }

    public Future $futureMethod() {
        System.out.println("! fut !"+Thread.currentThread());
        Promise<Object> promise = new Promise<>();
        delayed(10, () -> promise.resolve("Hello"));
        return promise;
    }

    public void $streamCallback( Callback cb ) {
        cb.stream("One");
        cb.stream("Two");
        cb.stream("Three");
        cb.finish();
    }

    public void $sporeTest(Spore<String, String> spore) {
        spore.remote("A");
        spore.remote("B");
        spore.finish();
    }

}
