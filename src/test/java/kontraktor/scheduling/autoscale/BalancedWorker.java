package kontraktor.scheduling.autoscale;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ruedi on 18.10.14.
 */
public class BalancedWorker extends Actor<BalancedWorker> {

    final ArrayList<String> items = new ArrayList();

    BalancedWorker connected[];

    public void $init( BalancedWorker ... siblings ) {
        connected = siblings;
    }

    public Future<Integer> $getItemCount() {
        return new Promise(items.size());
    }

    public Future $addItems(String it[]) {
        for (int i = 0; i < it.length; i++) {
            items.add(it[i]);
        }
        return new Promise<>("void");
    }

    public void $addItem(String s) {
        items.add(s);
    }

    public void $promote(int num) {
        int conIdx = 0;
        for ( int i = 0; i < num; i++ ) {
            if ( items.size() == 0 )
                return;
            connected[conIdx].$addItem(items.remove(items.size()-1));
            conIdx = (conIdx + 1) % connected.length;
        }
    }

}
