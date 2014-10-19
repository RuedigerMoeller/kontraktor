package org.nustaq.kontraktor.monitoring;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Future;

/**
 * Created by ruedi on 18.10.14.
 */
public interface Monitorable {

    public Future $getReport();
    public Future<Monitorable[]> $getSubMonitorables();

}
