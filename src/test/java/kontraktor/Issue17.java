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
            HelloActor myService = AsActor(HelloActor.class, 10000000);

            Thread tpub = new Thread() {
                public void run() {
                    try {
                        new TCPPublisher()
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
//                    .inboundQueueSize(1_000_000);

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

        // might reorder responses in contradiction to other solution
        static void callActorBetter(HelloActor act) {
            byte[] bb = new byte[500];
            long n = 1_000_000;
            try {
                while (true) {

                    for (long i = 0; i < n; i++) {
                        act.greet(bb).then((integer, err) -> {
                            // process result here
                            //
                        });
                        // apply some backpressure to avoid overly large queues and timeouts
                        if ( act.isMailboxPressured() )
                            LockSupport.parkNanos(1);
                    }
                    System.out.println("..");
                }
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }

        static void callActor_BP(HelloActor act) {
            byte[] bb = new byte[500];
            long n = 1_000_000;
            try {
                while (true) {

                    List<IPromise<Integer>> l = new ArrayList<>();
                    for (long i = 0; i < n; i++) {
                        l.add(act.greet(bb));
                        if ( act.isMailboxPressured() ) // apply some backpressure to avoid overly large queues and timeouts
                            LockSupport.parkNanos(1);
                    }

                    // easiest way, but stackoverflow with huge sample (await is stack based)
//                    awaitAll(l).forEach( i -> {
//                        if ( i == null )
//                            System.out.println("ERROR");
//                    });

                    // save agains stackoverflow
                    l.forEach( promise -> {
                        Integer i = promise.await();
                        if ( i == null )
                            throw new RuntimeException("ERROR");
                    });

                    System.out.println("..");
                }
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }

        // no BP version
        static void callActorNOBP(HelloActor act) {
            byte[] bb = new byte[500];
            long n = 1_000_000;
            try {
                while (true) {

                    List<IPromise<Integer>> l = new ArrayList<>();
                    for (long i = 0; i < n; i++) {
                        l.add(act.greet(bb));
                    }

                    l.forEach( promise -> {
                        Integer i = promise.await();
                        if ( i == null )
                            throw new RuntimeException("ERROR");
                    });

                    System.out.println("..");
                }
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }

        // complete original version
        static void callActor(HelloActor act) {
            byte [] bb = new byte[500];
            long n=1000000;
            try {
                while (true) {
                    long tim = System.currentTimeMillis();
                    List<IPromise<Integer>> l = new ArrayList<>();
                    for (long i = 0; i < n; i++) {
                        l.add(act.greet(bb));
                    }

                    l.forEach((integerIPromise) -> {
                        try{integerIPromise.await();}catch(Throwable th){th.printStackTrace();}
                    });

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

