package kontraktor.remoting.helpers;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 03.04.2015.
 */
public class MultiRefExposingActor extends Actor<MultiRefExposingActor> {

    public IPromise<ServiceA> createA() {
        return new Promise<>(AsActor(ServiceA.class, getScheduler()) );
    }

    public IPromise<ServiceB> createB() {
        return new Promise<>(AsActor(ServiceB.class, getScheduler()) );
    }

    public static class ServiceA extends Actor<ServiceA> {

        static AtomicInteger count = new AtomicInteger(0);

        int num = -1;

        public IPromise<Long> $fun() {
            return new Promise<>(System.currentTimeMillis());
        }

        public IPromise $stopFromRemote() {
            $stop();
            return new Promise<>("Done");
        }

        public void $endless() {
            if ( num < 0 ) {
                num = count.incrementAndGet();
            }
            System.out.println(getString() +num);
            delayed(3000, () -> {
                if ( !self().isStopped() )
                    self().$endless();
            });
        }

        protected String getString() {
            return "A ";
        }

    }

    public static class ServiceB extends ServiceA {

        protected String getString() {
            return "B ";
        }

    }


}
