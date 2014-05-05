package de.ruedigermoeller.abstraktor;

import java.util.concurrent.locks.LockSupport;

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

        public void service( String in, ActorFuture<String> result ) {
            if ( other != null )
                other.service(in, result);
            else
                result.receiveResult(in + "-result"+" in Thread "+Thread.currentThread().getName());
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

        for ( int i : new int[20] ) {
            bench(actorA);
        }


        final SampleActor actorB = Actors.AsActor(SampleActor.class);
        actorA.setOther(actorB);
        while( true ) {
            Thread.sleep(1000);
            actorA.service("Hallo", new ActorFuture<String>() {
                @Override
                public void receiveError(Object error) {
                    System.out.println("error "+error+" in Thread "+Thread.currentThread().getName());
                }

                @Override
                public void receiveResult(String result) {
                    System.out.println("forward result in "+Thread.currentThread().getName());
                    actorB.printStuff(result);
                }
            });
        }
    }

}
