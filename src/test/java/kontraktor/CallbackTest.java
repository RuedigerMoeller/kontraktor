package kontraktor;

import junit.framework.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.annotations.InThread;
import org.nustaq.kontraktor.impl.ElasticScheduler;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 14.06.14.
 */
public class CallbackTest {
    static AtomicInteger errors = new AtomicInteger(0);

    // callback from outside
    // nested callbacks
    // check no double threads in

    public static interface MyCB {
        public void cb( Object o);
    }

    public static class CBTActor extends Actor<CBTActor> {

        public void $method(Callback cb) {
//            LockSupport.parkNanos(1);
            cb.receiveResult("void", null);
        }

        public void $ping( CBTCallActor pong, Callback cb ) {
            assertTrue(Thread.currentThread() == __currentDispatcher);
            pong.$pong(self(),cb);
        }

        public void $pongong( Callback cb ) {
            assertTrue(Thread.currentThread() == __currentDispatcher);
            cb.receiveResult("yuppie","none");
        }

        public void $customCB( @InThread MyCB cb ) {
            cb.cb("Hallo");
        }

        public void $customCB1( MyCB cb ) {
            cb.cb("Hallo");
        }
    }

    public static class CBTCallActor extends Actor<CBTCallActor> {
        CBTActor cbt;

        public void $init() {
            cbt = Actors.AsActor(CBTActor.class, new ElasticScheduler(4,10000)); //  ensure different thread
            cbt.$method(new Callback() {
                @Override
                public void receiveResult(Object result, Object error) {
                    assertTrue(__currentDispatcher == Thread.currentThread());
                }
            });
            cbt.$customCB(new MyCB() {
                @Override
                public void cb(Object o) {
                    assertTrue(__currentDispatcher == Thread.currentThread());
                }
            });
            cbt.$customCB1(inThread(new MyCB() {
                @Override
                public void cb(Object o) {
                    assertTrue(__currentDispatcher == Thread.currentThread());
                }
            }));
        }

        @Override
        public void $stop() {
            cbt.$stop();
            super.$stop();
        }

        public void $sendPing() {
            cbt.$ping(self(), new Callback() {
                @Override
                public void receiveResult(Object result, Object error) {
                    assertTrue(Thread.currentThread() == __currentDispatcher);
                    assertTrue("yuppie".equals(result));
                    assertTrue("none".equals(error));
                }
            });
        }

        public void $pong( CBTActor ping, Callback cb ) {
            assertTrue(Thread.currentThread() == __currentDispatcher);
            ping.$pongong( cb );
        }

    }

    private static void assertTrue(boolean equals) {
        if ( ! equals )
            errors.incrementAndGet();
        Assert.assertTrue(equals);
    }

    @Test
    public void callbackTest() throws InterruptedException {
        CBTCallActor act = Actors.AsActor(CBTCallActor.class);
        act.$init();
        for (int i = 0; i < 10000; i++) // higher vals create deadlock, detect it !
        {
            act.$sendPing();
//            if ( (i % 1000) == 0 ) {
//                System.out.println("POK --");
//            }
//            LockSupport.parkNanos(1000);
        }
        final CBTActor cbt = Actors.AsActor(CBTActor.class);
        cbt.$method(new Callback() {
            @Override
            public void receiveResult(Object result, Object error) {
                assertTrue(Thread.currentThread() == cbt.__currentDispatcher);
            }
        });
        Thread.sleep(500);
        cbt.$stop();
        act.$stop();
        assertTrue(errors.get() == 0);
    }

}