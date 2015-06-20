package org.nustaq.kontraktor;

import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPPublisher;

/**
 * Created by ruedi on 19/06/15.
 */
public class RefChain {

    public static class A extends Actor<A> {
        public IPromise showChain( ConnectableActor b ) {
            B bref = (B) b.connect(null).await();
            C cref = bref.getC().await();
            String pok = cref.hello("POK").await();
            System.out.println("received "+pok);
            return new Promise(null);
        }
    }

    public static class B extends Actor<A> {
        C c;
        public void init(ConnectableActor connectable) {
            connectable.connect( null ).then( c -> this.c = (C) c);
        }
        public IPromise<C> getC() {
            return new Promise<>(c);
        }
    }

    public static class C extends Actor<A> {
        public IPromise<String> hello(String s) {
            return new Promise<>("Hello:"+s);
        }
    }

    public static void main(String[] args) throws InterruptedException {

        // though a,b,c run inside single process use remote refs to interconnect
        A a = Actors.AsActor(A.class);
        B b = Actors.AsActor(B.class);
        C c = Actors.AsActor(C.class);

        new TCPPublisher(a, 4001).publish();
        new TCPPublisher(b, 4002).publish();
        new TCPPublisher(c, 4003).publish();

        ConnectableActor cConnect = new TCPConnectable(C.class,"localhost",4003);
        ConnectableActor bConnect = new TCPConnectable(B.class,"localhost",4002);

        b.init(cConnect);
        Thread.sleep(500); // don't program like this, init should return promise ..

        a.showChain(bConnect).await();

        a.stop();
        b.stop();
        c.stop();
        System.exit(0);
    }

}
