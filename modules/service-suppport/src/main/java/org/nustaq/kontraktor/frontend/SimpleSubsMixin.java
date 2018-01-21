package org.nustaq.kontraktor.frontend;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.reallive.api.ChangeMessage;

public interface SimpleSubsMixin {

    SubsRegistry getReg();

    default void query(String tbl, String query, Callback<ChangeMessage> cb) {
        getReg().query(tbl,query,null,cb);
    }

    default IPromise<Integer> getSubsId() {
        return new Promise<>(getReg().getSubsId());
    }

    default void subscribe(int subsId, String tableName, String filter, Callback<ChangeMessage> changeReceiver) {
        getReg().subscribe(subsId,tableName,filter,null,changeReceiver);
    }

    default void unsubscribe(int subsid) {
        getReg().unsubscribe(subsid);
    }

}
