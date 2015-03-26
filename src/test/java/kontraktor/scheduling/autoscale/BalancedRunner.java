package kontraktor.scheduling.autoscale;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.impl.ElasticScheduler;
import org.nustaq.kontraktor.util.Hoarde;
import org.nustaq.kontraktor.util.HttpMonitor;

import java.util.Random;

/**
 * Created by ruedi on 18.10.14.
 */
public class BalancedRunner extends Actor<BalancedRunner> {

    Hoarde<BalancedWorker> h;
    Random r;

    public void $main() {
        r = new Random(13);
        ElasticScheduler scheduler = new ElasticScheduler(8);
        HttpMonitor.getInstance().$publish("scheduler",scheduler);
        HttpMonitor.getInstance().$publish("mainscheduler",getScheduler());
        h = new Hoarde<BalancedWorker>(800,BalancedWorker.class, scheduler);
        h.each( (worker,i) -> worker.$init( h.getActor((i+1)%h.getSize()), h.getActor((i+2)%h.getSize()) ) );
        for (int i = 0; i < 100000; i++) {
            h.getActor(0).$addItem(""+i);
        }
        $run();
        $report();
    }

    public void $run() {
        h.each( (worker) -> worker.$promote(r.nextInt(4000)));
        delayed( 1, () -> $run() );
    }

    public void $report() {
        all(h.map((worker, i) -> worker.$getItemCount())).then( (futures,err) -> {
            int count = 0;
            for (int i = 0; i < futures.length; i++) {
                Future future = futures[i];
                if ( ((Number)future.get()).intValue() > 0 ) {
                    count++;
                }
            }
            System.out.println("actors with stuff: "+count);
        });
        delayed( 2000, () -> $report() );
    }


    public static void main(String arg[]) {
        BalancedRunner runner = Actors.AsActor(BalancedRunner.class);
        runner.$main();
    }
}
