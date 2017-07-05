package org.nustaq.sometest;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.http.undertow.HttpPublisher;

/**
 * Created by ruedi on 05.07.17.
 */
public class TestActeur extends Actor<TestActeur> {

    public void plain( String arg, int arg1 ) {
        System.out.println("plain "+arg+" "+arg1);
    }

    public IPromise<String> plainPromise(String arg, int arg1 ) {
        String x = "plainPromise " + arg + " " + arg1;
        System.out.println(x);
        return new Promise<>(x);
    }

    public IPromise<String> plainCallback(String arg, int arg1, Callback cb ) {
        String x = "plainCallback " + arg + " " + arg1;
        System.out.println(x);
        cb.stream(arg).stream(arg1).finish();
        return new Promise<>(x);
    }

    public static void main(String[] args) {
        new HttpPublisher(Actors.AsActor(TestActeur.class),"localhost", "/test", 8888).publish( x -> System.out.println("DISCONNECTED:"+x));
    }
}
