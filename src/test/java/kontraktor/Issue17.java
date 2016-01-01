package kontraktor;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.remoting.tcp.TCPPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import static org.nustaq.kontraktor.Actors.*;

/**
 * Created by ruedi on 13/10/15.
 */
public class Issue17 {


    static public class HelloActor extends Actor<HelloActor> {
        public IPromise<Integer> greet(byte[] b) {
            return new Promise<>(b.length);
        }
    }

    public static class Main {

        public static void main(String[] args) throws InterruptedException {
            testRemote();
        }

        static void testRemote() throws InterruptedException {
            HelloActor myService = AsActor(HelloActor.class, 1_000_000);

            Thread tpub = new Thread() {
                public void run() {
                    try {
                        new TCPNIOPublisher() // note TCPPublisher consumes more CPU
                            .facade(myService)
                            .port(8181)
                            .serType(SerializerType.FSTSer)
                            .publish();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };
            tpub.start();

            ConnectableActor con =
                new TCPConnectable(HelloActor.class, "localhost", 8181);

            Callback<ActorClientConnector> disconnectCallback = new Callback<ActorClientConnector>() {
                @Override
                public void complete(ActorClientConnector actorClientConnector, Object o) {
                    System.out.println("disconnect");
                }
            };

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            HelloActor act = (HelloActor) con.connect(disconnectCallback).await();

            Thread thmain = new Thread() {
                public void run() {
                    callActor(act);
                }
            };

            thmain.setName("caller");
            thmain.start();

            Thread thmain2 = new Thread() {
                public void run() {
                    callActor(act);
                }
            };
            thmain2.setName("caller2");
            thmain2.start();

            thmain.join();
            thmain2.join();
            System.out.println("main threads finished");
        }

        // modified original version, somewhat flawed as creates a lot of queuing
        // by sending batches of 1 million requests (=1 million calback ids createdm hashed, and removed)
        //
        static void callActor(HelloActor act) {
            byte [] bb = new byte[500];
            long n=1_000_000; // reduction to 200k increases throughput significantly (less queuing and callback management)
            try {
                while (true) {
                    long tim = System.currentTimeMillis();
                    List<IPromise<Integer>> l = new ArrayList<>();
                    for (long i = 0; i < n; i++) {
                        l.add(act.greet(bb));
                    }

// slow:
//                    l.forEach((integerIPromise) -> {
//                        try{integerIPromise.await();}catch(Throwable th){th.printStackTrace();}
//                    });
                    all(l).await(30_000); // better, even better would be elimination of await() using 'then()'

                    l.forEach(p -> {
                        try {
                            p.get();
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                    });
                    System.out.println(""+(System.currentTimeMillis()-tim));
                }
            } catch(Throwable th){
                th.printStackTrace();
            }
        }

    }

    public static void main(String[] args) throws InterruptedException {
        Main.main(args);
    }
}

