package kontraktor;

import de.ruedigermoeller.kontraktor.*;
import de.ruedigermoeller.kontraktor.Promise;

import java.util.concurrent.Callable;

/**
 * Created by moelrue on 05.05.2014.
 */
public class Playground {

    public static class SampleActor extends Actor<SampleActor> {

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
            async( new Callable<String>() {
                        public String call() throws Exception {
                return "TEST"; //new Scanner(new URL("http://www.spiegel.de").openStream(), "UTF-8").useDelimiter("\\A").next();
            }              }).then(new Callback<String>() {
                public void receiveResult(String result, Object error) {
                if (t != Thread.currentThread()) {
                    System.out.println("Ooops !");
                }
                System.out.println("received website id:" + id + " size:" + result.length());
                }
            });
        }

        public void service( String in, Callback<String> result ) {
            if ( other != null )
                other.service(in, result);
            else
                result.receiveResult(in + "-result"+" in Thread "+Thread.currentThread().getName(),null);
        }

        public Future<String> getFutureString() {
            System.out.println("getfutstring thread "+System.identityHashCode(Thread.currentThread()));
            return new Promise<>("FString");
        }

        public Future<String> concat(final Future<String> pokpok) {
            final Future<String> resultFuture = new Promise();
            final Thread curt = Thread.currentThread();
            pokpok.then(new Callback<String>() {
                @Override
                public void receiveResult(String result, Object error) {
                    if (Thread.currentThread()!=curt) throw new RuntimeException("wrong thread");
                    System.out.println("concat thread "+System.identityHashCode(curt));
                    result+="POKPOK";
                    resultFuture.receiveResult(result,null);
                }
            });
            return resultFuture;
        }
    }

    private static void bench(SampleActor actorA) {
        long tim = System.currentTimeMillis();
        int numCalls = 1000 * 1000 * 10;
        for ( int i = 0; i < numCalls; i++ ) {
            actorA.emptyMethod("A", "B", "C");
        }
        try {
            Thread.sleep(1000 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("tim "+(numCalls/(System.currentTimeMillis()-tim))*1000+" calls per sec");
    }

    public static void main( String arg[] ) throws InterruptedException {

        SampleActor actorA = Actors.AsActor(SampleActor.class);
        final SampleActor actorB = Actors.AsActor(SampleActor.class);
        actorA.setOther(actorB);
        for ( int i : new int[10] ) {
            bench(actorA);
        }

        final Future<String> futureString = actorA.getFutureString();
        actorB.concat(futureString).then(new Callback<String>() {
            @Override
            public void receiveResult(String result, Object error) {
                System.out.println("uter result thread "+System.identityHashCode(Thread.currentThread()));
                System.out.println("result:" + result);
            }
        });


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
