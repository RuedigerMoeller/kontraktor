package kontraktor;

import org.junit.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.KConnectionPool;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class ConPoolTest {

    public static class RActor extends Actor<RActor> {
        public IPromise<String> hello(String s) {
            return resolve("Hello "+s);
        }
    }

    @Test
    public void test() throws InterruptedException {
        RActor act = Actors.AsActor(RActor.class);
        new TCPNIOPublisher().port(9876).facade(act).publish( a -> {
            System.out.println("disconnect "+a);
        });

        TCPConnectable con = new TCPConnectable(RActor.class,"localhost",9876);

        KConnectionPool pool = new KConnectionPool();

        AtomicReference resObj = new AtomicReference();

        pool.getConnection(con).then( remote -> {
            System.out.println("REMOTE "+remote);
            checkRef(""+remote,resObj);
        });

        pool.getConnection(con).then( remote -> {
            System.out.println("REMOTE "+remote);
            checkRef(""+remote,resObj);
        });
        pool.getConnection(con).then( remote -> {
            System.out.println("REMOTE "+remote);
            checkRef(""+remote,resObj);
        });
        pool.getConnection(con).then( remote -> {
            System.out.println("REMOTE "+remote);
            checkRef(""+remote,resObj);
        });
        pool.getConnection(con).then( remote -> {
            System.out.println("REMOTE "+remote);
            checkRef(""+remote,resObj);
        });

        Thread.sleep(1000);
        pool.getConnection(con).then( remote -> {
            System.out.println("REMOTE late "+remote);
            checkRef(""+remote,resObj);
        });
        Thread.sleep(1000);

    }

    private void checkRef(String s, AtomicReference resObj) {
        if ( resObj.get() != null )
            Assert.assertTrue(resObj.get().equals(s));
        else
            resObj.set(s);
    }
}
