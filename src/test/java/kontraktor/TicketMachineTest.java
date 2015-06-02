package kontraktor;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.util.TicketMachine;
import junit.framework.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 02.06.14.
 */
public class TicketMachineTest {

    public static class AsyncWork extends Actor<AsyncWork> {

        public IPromise work(final long nanos) {
            Promise promise = new Promise<>();
            exec(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    LockSupport.parkNanos(nanos);
                    return "void";
                }
            }).then(promise);
            return promise;
        }
    }

    static AtomicInteger errorCount = new AtomicInteger(0);
    public static class TicketedProcessor extends Actor<TicketedProcessor> {

        TicketMachine machine;
        AsyncWork worker;
        HashMap<Object,Long> seqTracker;

        public void init() {
            machine = new TicketMachine();
            worker = Actors.AsActor(AsyncWork.class);
            seqTracker = new HashMap<>();
        }

        public void process( final String stock, final long sequence ) {
            machine.getTicket(stock).then(new Callback<IPromise>() {
                @Override
                public void complete(final IPromise finSignal, Object error) {
                    Long curSeq = seqTracker.get(stock);
                    if ( curSeq == null )
                        seqTracker.put(stock,curSeq);
                    else {
                        if ( curSeq.longValue() != sequence-1 ) {
                            System.out.println("***** error on " + stock + " prevSeq " + curSeq + " new Seq " + sequence);
                            errorCount.incrementAndGet();
                        }
                    }
//                    System.out.println("working "+stock+" sq:"+sequence);
                    worker.work((long) (Math.random()*1000*1000)).then(new Callback() {
                        @Override
                        public void complete(Object result, Object error) {
                            System.out.println("fin work "+stock+" "+sequence);
                            finSignal.complete("done", null); // tell i am done
                        }
                    });
                }
            });
        }

        @CallerSideMethod TicketMachine getMachindeForTest() {
            return getActor().machine;
        }

    }

    @Test
    public void test() throws InterruptedException {
        TicketedProcessor proc = Actors.AsActor(TicketedProcessor.class);
        proc.init();
        String stocks[] = { "ALV", "BMW", "FDAX", "ODAX", "FGBL", "CCIP", "OGBL" };
        int seq[] = new int[stocks.length];
        for( int n = 0; n < 10000; n++) {
            int index = (int) (Math.random() * stocks.length);
            proc.process(stocks[index], seq[index]++ );
            LockSupport.parkNanos(1000*100);
        }
        Thread.sleep(1000*5);
        Assert.assertTrue(proc.getMachindeForTest().getTickets().size() == 0);
        Assert.assertTrue(errorCount.get() == 0);
    }

}
