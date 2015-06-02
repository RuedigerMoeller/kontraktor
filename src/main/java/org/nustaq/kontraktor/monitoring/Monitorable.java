package org.nustaq.kontraktor.monitoring;

import org.nustaq.kontraktor.IPromise;

/**
 * Created by ruedi on 18.10.14.
 */
public interface Monitorable {

    public IPromise getReport();
    public IPromise<Monitorable[]> getSubMonitorables();

}
