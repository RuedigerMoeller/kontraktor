package kontraktor;

import de.ruedigermoeller.kontraktor.*;
import de.ruedigermoeller.kontraktor.util.TicketMachine;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 02.06.14.
 */
public class TicketMachineTest {

    public static class AsyncWork extends Actor<AsyncWork> {

        public Future $work(final long millis) {
            Promise promise = new Promise<>();
            Actors.Async( new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    try {
                        Thread.sleep(millis);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return "void";
                }
            }).then(promise);
            return promise;
        }
    }

    public static class TicketedProcessor extends Actor<TicketedProcessor> {

        TicketMachine machine;
        AsyncWork worker;
        HashMap<Object,Long> seqTracker;

        public void $init() {
            machine = new TicketMachine();
            worker = Actors.SpawnActor(AsyncWork.class);
            seqTracker = new HashMap<>();
        }

        public void $process( final String stock, final long sequence ) {
            machine.getTicket(stock).then(new Callback<Future>() {
                @Override
                public void receiveResult(final Future finSignal, Object error) {
                    Long curSeq = seqTracker.get(stock);
                    if ( curSeq == null )
                        seqTracker.put(stock,curSeq);
                    else {
                        if ( curSeq.longValue() != sequence-1 )
                            System.out.println("***** error on "+stock+" prevSeq "+curSeq+" new Seq "+sequence);
                    }
//                    System.out.println("working "+stock+" sq:"+sequence);
                    worker.$work((long) (Math.random()*1000)).then(new Callback() {
                        @Override
                        public void receiveResult(Object result, Object error) {
                            System.out.println("fin work "+stock+" "+sequence);
                            finSignal.receiveResult("done", null); // tell i am done
                        }
                    });
                }
            });
        }

    }

    public static void main(String arg[]) {
        TicketedProcessor proc = Actors.AsActor(TicketedProcessor.class);
        proc.$init();
        String stocks[] = { "ALV", "BMW", "FDAX", "ODAX", "FGBL", "CCIP", "OGBL" };
        int seq[] = new int[stocks.length];
        while( true ) {
            int index = (int) (Math.random() * stocks.length);
            proc.$process(stocks[index], seq[index]++ );
            LockSupport.parkNanos(1000*1000*200);
        }
    }

}
