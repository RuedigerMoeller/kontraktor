package kontraktor;

import de.ruedigermoeller.kontraktor.Actor;
import de.ruedigermoeller.kontraktor.Actors;
import de.ruedigermoeller.kontraktor.Callback;

import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 * Created by moelrue on 05.05.2014.
 */
public class Playground {

    public static class SampleActor extends Actor {

        SampleActor other;
        int callCount;

        public SampleActor() {
        }

        public void emptyMethod( Object arg, Object arg1, Object arg2 ) {
            callCount++;
        }

        public void setOther(SampleActor actor) {
            other = actor;
        }

        public void printStuff( String stuff ) {
            System.out.println(stuff+" in Thread "+Thread.currentThread().getName());
        }

        public void doBlockingStuff( final String id ) {
            final Thread t = Thread.currentThread();
            Actors.Execute(
                    new Callable<String>() {
                        public String call() throws Exception {
                            return "TEST"; //new Scanner(new URL("http://www.spiegel.de").openStream(), "UTF-8").useDelimiter("\\A").next();
                        }
                    },
                    new Callback<String>() {
                        public void receiveResult(String result, Object error) {
                            if (t != Thread.currentThread()) {
                                System.out.println("Ooops !");
                            }
                            System.out.println("received website id:" + id + " size:" + result.length());
                        }
                    }
            );
        }

        public void service( String in, Callback<String> result ) {
            if ( other != null )
                other.service(in, result);
            else
                result.receiveResult(in + "-result"+" in Thread "+Thread.currentThread().getName(),null);
        }

    }

    private static void bench(SampleActor actorA) {
        long tim = System.currentTimeMillis();
        int numCalls = 1000 * 1000 * 10;
        for ( int i = 0; i < numCalls; i++ ) {
            actorA.emptyMethod("A", "B", "C");
        }
        actorA.getDispatcher().waitEmpty(1000*1000);
        System.out.println("tim "+(numCalls/(System.currentTimeMillis()-tim))*1000+" calls per sec");
    }

    public static void main( String arg[] ) throws InterruptedException {
        SampleActor actorA = Actors.AsActor(SampleActor.class);

//        for ( int i : new int[10] ) {
//            bench(actorA);
//        }

        final SampleActor actorB = Actors.AsActor(SampleActor.class);
        actorA.setOther(actorB);
        int count = 0;
        for ( int i : new int[10])
        {
            Thread.sleep(1000);
            actorA.service("Hallo", new Callback<String>() {
                @Override
                public void receiveResult(String result, Object error) {
                    System.out.println("forward result in "+Thread.currentThread().getName());
                    actorB.printStuff(result);
                }
            });
            actorB.doBlockingStuff(""+count++);
        }
        actorA.stop(); actorB.stop();
        Thread.sleep(2000);
    }

}
