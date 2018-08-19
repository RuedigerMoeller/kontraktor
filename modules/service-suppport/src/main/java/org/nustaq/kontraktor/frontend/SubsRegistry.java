package org.nustaq.kontraktor.frontend;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.services.rlclient.DataClient;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.api.*;
import org.nustaq.reallive.impl.RLUtil;
import org.nustaq.reallive.messages.AddMessage;
import org.nustaq.reallive.messages.QueryDoneMessage;
import org.nustaq.reallive.query.QParseException;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * builds up real remote queries from searchfilter objects.
 */
public class SubsRegistry {

    AtomicInteger subsIdCount = new AtomicInteger(1);
    HashMap<Integer, FrontEndSubscription> subsMap = new HashMap<Integer, FrontEndSubscription>();
    DataClient dclient;
    boolean terminated = false;
    Function<String,ChangeStream> tableFactory = tableName -> (ChangeStream) dclient.tbl(tableName);

    // fixme: currently no cleanup here
    public SubsRegistry(DataClient dclient) {
        this.dclient = dclient;
    }

    public void query(String tableName, String filter, String reducedFields[], Callback<ChangeMessage> changeReceiver ) {
        if (filter == null || filter.trim().length() == 0 || filter.trim().equals("true"))
            filter = "1";
        RealLiveTable table = (RealLiveTable) tableFactory.apply(tableName);
        if (table == null) {
            changeReceiver.complete(null, "unknown table");
        } else {
            try {
                table.query(filter, (record,error) -> {
                    if ( record != null ) {
                        if (reducedFields != null && reducedFields.length > 0) {
                            record = record.reduced(reducedFields);
                        }
                        changeReceiver.pipe(new AddMessage(record));
                    } else {
                        if (Actors.isError(error) ) {
                            changeReceiver.reject(error);
                        } else
                            changeReceiver.resolve(new QueryDoneMessage());
                    }
                });
            } catch (QParseException e) {
                changeReceiver.reject("ParseException:" + e.getMessage());
                Log.Error(this,e);
                return;
            }
        }
    }

    /**
     * simple subscribe/query with filter string
     */
    public void subscribe(int subsId, String tableName, String filter, String reducedFields[], Callback<ChangeMessage> changeReceiver) {
        if (filter == null || filter.trim().length() == 0 || filter.trim().equals("true"))
            filter = "1";
        ChangeStream table = tableFactory.apply(tableName);
        if (table == null) {
            changeReceiver.complete(null, "unknown table");
        } else {
            Subscriber subscriber;
            try {
                subscriber = table.subscribeOn(filter, change -> {
                    if (reducedFields != null && reducedFields.length > 0) {
                        change = change.reduced(reducedFields);
                    }
                    changeReceiver.pipe(change);
                });
            } catch (QParseException e) {
                changeReceiver.reject("ParseException:" + e.getMessage());
                Log.Error(this,e);
                return;
            }
            subsMap.put(subsId, new FrontEndSubscription(subscriber, tableName, changeReceiver));
        }
    }

    public void unsubscribe(int subsId) {
        FrontEndSubscription feSubs = subsMap.get(subsId);
        if (feSubs != null) {
            RealLiveTable table = (RealLiveTable) dclient.tbl(feSubs.getTableName());
            table.unsubscribe(feSubs.getSubscriber());
            subsMap.remove(subsId);
            feSubs.getFrontEndCallback().finish(); // FIXME: double finish sent ?
        }
    }

    public int getSubsId() {
        return subsIdCount.incrementAndGet();
    }

    public void setTableFactory( Function<String,ChangeStream> factory )
    {
        tableFactory = factory ;
    }

    public DataClient getDataClient()
    {
        return dclient;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void setTerminated(boolean iAmDead) {
        this.terminated = iAmDead;
    }

    public void unsubscribeAll() {
        subsMap.keySet().stream().collect(Collectors.toList()).forEach( k -> this.unsubscribe(k) );
    }
}
