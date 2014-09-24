package kontraktor.scheduling;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.util.Hoarde;

import java.util.ArrayList;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 24.09.14.
 */
public class Dining {

    public static class TableCoordinator extends Actor<TableCoordinator> {
        ArrayList<Promise> forks[] = new ArrayList[5];

        public TableCoordinator() {
            for (int i = 0; i < forks.length; i++) {
                forks[i] = new ArrayList<>();
            }
        }

        public Future getFork( int num ) {
            num = num % 5;
            Promise res;
            if ( forks[num].size() == 0 ) {
                res = new Promise("void");
            } else {
                res = new Promise();
            }
            forks[num].add(res);
            return res;
        }

        public void returnFork(int num) {
            num = num % 5;
            forks[num].remove(0);
            if ( forks[num].size() > 0 )
                forks[num].get(0).receive("void",null);
        }
    }

    public static class Philosopher extends Actor<Philosopher> {
        String name;
        int nr;
        TableCoordinator tableCoordinator;
        volatile String state;

        public void $start(String name, int nr, TableCoordinator tableCoordinator) {
            this.name = name;
            this.nr = nr;
            this.tableCoordinator = tableCoordinator;
            $think();
        }

        public void $think() {
            long thinkTime = (long) (5000 * Math.random());
            state = "Think";
            delayed(thinkTime, () -> {
                state = "Hungry";
                yield( tableCoordinator.getFork(nr), tableCoordinator.getFork(nr + 1) ).then((r, e) -> {
                    state = "Eat";
                    delayed(2000, () -> {
                        tableCoordinator.returnFork(nr);
                        tableCoordinator.returnFork(nr + 1);
                        self().$think();
                    });
                });
            });
        }

        @CallerSideMethod String getState() {
            return getActor().name+" "+getActor().state;
        }
    }

    public static void main( String arg[] ) {
        String names[] = { "A", "B", "C", "D", "E" };
        TableCoordinator tableCoordinator = Actors.AsActor(TableCoordinator.class);
        Hoarde<Philosopher> phils = new Hoarde<>( 5, Philosopher.class );
        phils.each( (phil, index) -> phil.$start(names[index], index, tableCoordinator));
        new Thread(() -> {
            while( true ) {
                LockSupport.parkNanos(1000 * 1000l * 1000);
                phils.each((phil) -> System.out.print(phil.getState() + ", ") );
                System.out.println();
            }
        }).start();
    }
}
