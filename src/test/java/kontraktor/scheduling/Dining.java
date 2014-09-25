package kontraktor.scheduling;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.impl.ElasticScheduler;
import org.nustaq.kontraktor.util.Hoarde;

import java.util.ArrayList;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 24.09.14.
 */
public class Dining {

    public static class Coordinator extends Actor<Coordinator> {
        ArrayList<Promise> forks[] = new ArrayList[5];

        public Coordinator() {
            for (int i = 0; i < forks.length; i++)
                forks[i] = new ArrayList<>();
        }

        public Future getFork( int num ) {
            num %= 5;
            Promise res = forks[num].size() == 0 ? new Promise("void") : new Promise();
            forks[num].add(res);
            return res;
        }

        public void returnFork(int num) {
            num %= 5;
            forks[num].remove(0);
            if ( forks[num].size() > 0 )
                forks[num].get(0).signal();
        }
    }

    public static class Philosopher extends Actor<Philosopher> {
        String name;
        int nr;
        Coordinator coordinator;
        String state;
        int eatCount;

        public void $start(String name, int nr, Coordinator coordinator) {
            this.name = name;
            this.nr = nr;
            this.coordinator = coordinator;
            $think();
        }

        public void $think() {
            long thinkTime = (long) (100 * Math.random());
            state = "Think";
            int firstFork, secondFork;
            if ( (nr & 1) == 0 ) {
                firstFork = nr; secondFork = nr+1;
            } else {
                firstFork = nr+1; secondFork = nr;
            }
            delayed(thinkTime, () -> {
                state = "Hungry";
                coordinator.getFork(firstFork).then((r, e) -> {
                    coordinator.getFork(secondFork).then((r1, e1) -> {
                        state = "Eat";
                        long eatTime = (long) (100 * Math.random());
                        delayed(eatTime, () -> {
                            eatCount++;
                            coordinator.returnFork(firstFork);
                            coordinator.returnFork(secondFork);
                            self().$think();
                        });
                    });
                });
            });
        }

        public Future<String> $getState() {
            return new Promise( name+" "+state+" eaten:"+eatCount );
        }
    }

    public static void main( String arg[] ) {
        String names[] = { "A", "B", "C", "D", "E" };
        Coordinator coordinator = Actors.AsActor(Coordinator.class);
        Hoarde<Philosopher> phils = new Hoarde<>( 5, Philosopher.class, new ElasticScheduler(1));

        phils.each( (phil, index) -> phil.$start(names[index], index, coordinator));

        // start a thread reporting state each second
        new Thread(() -> {
            while( true ) {
                LockSupport.parkNanos(1000 * 1000l * 1000);
                Actors.yield( phils.map( (phil, index) -> phil.$getState() ) ).then( (futs, e) -> {
                    for (int i = 0; i < futs.length; i++)
                        System.out.print(futs[i].getResult() + ", ");
                    System.out.println();
                });
            }
        }).start();
    }
}
