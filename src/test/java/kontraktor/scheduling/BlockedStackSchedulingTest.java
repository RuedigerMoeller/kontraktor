package kontraktor.scheduling;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.impl.DispatcherThread;
import org.nustaq.kontraktor.impl.ElasticScheduler;

/**
 * Created by ruedi on 23.09.14.
 */
public class BlockedStackSchedulingTest {

    public static class Master extends Actor<Master> {

        Slave slaves[];

        public void $init(int slaveNum) {
            slaves = new Slave[slaveNum];
            ElasticScheduler elasticScheduler = new ElasticScheduler(1, 4);
            for (int i = 0; i < slaves.length; i++) {
                slaves[i] = Actors.AsActor(Slave.class,elasticScheduler);
                slaves[i].$init();
            }
        }

        public void $feed() {
            for (int i = 0; i < slaves.length; i++) {
                Slave slave = slaves[i];
                slave.$doWorkerCalls(i);
            }
        }

    }

    public static class Slave extends Actor<Slave> {

        Worker worker;

        public void $init() {
            worker = Actors.AsActor(Worker.class,4);
        }

        public void $doWorkerCalls(int i) {
            worker.$work(1000*(i*4+1),i);
            System.out.println("slave finished job "+i);
        }

    }

    public static class Worker extends Actor<Slave> {

        public void $work(long i, int act) {
            try {
                Thread.sleep(i);
//                System.out.println("worker finished work "+act);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public static void main(String arg[]) {
        DispatcherThread.NUMBER_OF_MESSAGES_TO_PROCESS_PER_CHECK_FOR_NEW_ADDS = 1;
        Master m = Actors.AsActor(Master.class,new ElasticScheduler(1,4));
        m.$init(2);
        while( true ) {
            m.$feed();
            System.out.println("called $feed");
        }
    }
}
