package de.ruedigermoeller.abstraktor;

/**
 * Created by moelrue on 05.05.2014.
 */
public class Playground {

    public static class SampleActor extends Actor {

        SampleActor other;

        public SampleActor() {
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

    public static void main( String arg[] ) throws InterruptedException {
        SampleActor actorA = Actors.AsActor(SampleActor.class);
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
