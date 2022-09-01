package org.nustaq.utils;

import org.nustaq.kontraktor.remoting.base.TrafficMonitor;

public class TrafficMonitorUtil {
    public static void monitorTraffic(TrafficMonitor trafficMonitor, String sid, String direction, String path, int length) {
        if( trafficMonitor == null )  {
            return;
        }

        switch (direction) {
            case "in":
                trafficMonitor.requestReceived(length, sid, path);
                break;
            case "out":
                trafficMonitor.responseSend(length, sid, path);
                break;
            default:
                throw new IllegalArgumentException("direction must be 'in' or 'out'");
        }
    }
}
