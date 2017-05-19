package kontraktor.scheduling;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;

/**
 * Created by ruedi on 24.09.14.
 */
public class DeadLocks {

    public static class SelfDeadLocker extends Actor<SelfDeadLocker> {
        boolean signaled = false;
        volatile int msgcount = 0;

        public void deadLock( IPromise signal, boolean controlFlow ) {
            msgcount++;
            if ( msgcount < 10000 ) {
                if (controlFlow && isMailboxPressured()) {
                    delayed( 1, () -> self().deadLock(signal,controlFlow) );
                } else {
                    self().deadLock(signal, controlFlow);
                    self().deadLock(signal, controlFlow);
                }
            } else {
                if ( ! signaled ) {
                    signaled = true;
                    signal.resolve();
                }
            }
        }

    }

    public static void main( String arg[] ) {
        SelfDeadLocker dl = Actors.AsActor(SelfDeadLocker.class,1000);
        Promise end = new Promise();
        dl.deadLock(end,true);
        end.then((r, e) -> {
            System.out.println("control finished: "+dl.getActor().msgcount);
            dl.stop();
        });

        SelfDeadLocker dl1 = Actors.AsActor(SelfDeadLocker.class,1000);
        Promise end1 = new Promise();
        dl1.deadLock(end1, false);
        end1.then((r, e) -> {
            System.out.println("nocontrol finished: "+dl1.getActor().msgcount);
            dl1.stop();
        });

    }

}
