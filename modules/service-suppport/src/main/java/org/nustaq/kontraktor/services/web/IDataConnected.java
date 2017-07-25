package org.nustaq.kontraktor.services.web;

import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.services.rlclient.DataClient;
import org.nustaq.reallive.interfaces.RealLiveTable;

/**
 * Created by ruedi on 29.05.17.
 */
public interface IDataConnected {

    @CallerSideMethod
    DataClient getDataClient();

    @CallerSideMethod
    default RealLiveTable getTable(String name) {
        return getDataClient().getTableSync(name);
    }

    @CallerSideMethod
    default String getUserTableName() {
        return "user";
    }

    @CallerSideMethod
    default String getConfirmationTableName() {
        return "confirmation";
    }


}
