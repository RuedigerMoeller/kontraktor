package org.nustaq.kontraktor.examples;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.tcp.TCPActorClient;
import org.nustaq.kontraktor.remoting.tcp.TCPActorServer;
import org.nustaq.kontraktor.util.Hoarde;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 24.09.14.
 *
 * Solution of the Dining Philosopher Problem using actors. (no attempt is made on being fair)
 */
public class Dining {

    public static class Table extends Actor<Table> {
        ArrayList<Promise> forks[] = new ArrayList[5];

        public Table() {
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
        Table table;
        String state;
        int eatCount;

        public void $start(String name, int nr, Table table) {
            this.name = name;
            this.nr = nr;
            this.table = table;
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
                table.getFork(firstFork).then( (r, e) ->
                    table.getFork(secondFork).then( (r1, e1) -> {
                        state = "Eat";
                        long eatTime = (long) (100 * Math.random());
                        delayed(eatTime, () -> {
                            eatCount++;
                            table.returnFork(firstFork);
                            table.returnFork(secondFork);
                            self().$think();
                        });
                    })
                );
            });
        }

        public Future<String> $getState() {
            return new Promise( name+" "+state+" eaten:"+eatCount );
        }
    }

    static void runPhilosophers(Table coord) {
        String names[] = { "A", "B", "C", "D", "E" };
        Hoarde<Philosopher> phils = new Hoarde<>( 5, Philosopher.class ).each( (phi, i) -> phi.$start(names[i], i, coord) );
        startReportingThread(phils);
    }

    public static void runServer() {
        try {
            TCPActorServer.Publish(Actors.AsActor(Table.class), 6789);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void runClient() throws IOException {
        TCPActorClient.Connect(Table.class, "localhost", 6789 ).then((coord, e) -> {
            if (coord != null) {
                runPhilosophers(coord);
            } else {
                System.out.println("error:" + e);
            }
        });
    }

    public static void main( String arg[] ) throws IOException {
        if ( arg.length == 0 ) {
            runServer();
        } else if (arg.length == 1 ) {
            runClient();
        } else {
            // run them in process
            runPhilosophers(Actors.AsActor(Table.class));
        }
    }

    private static void startReportingThread(Hoarde<Philosopher> phils) {
        // start a thread reporting state each second
        new Thread(() -> {
            while( true ) {
                LockSupport.parkNanos(1000 * 1000l * 1000);
                Actors.yield(phils.map((phil, index) -> phil.$getState())).then( (futs, e) -> {
                    for (int i = 0; i < futs.length; i++)
                        System.out.print(futs[i].getResult() + ", ");
                    System.out.println();
                });
            }
        }).start();
    }
}
