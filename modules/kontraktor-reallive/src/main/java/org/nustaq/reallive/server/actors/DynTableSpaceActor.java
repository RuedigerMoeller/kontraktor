package org.nustaq.reallive.server.actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.reallive.server.actors.RealLiveTableActor;
import org.nustaq.reallive.server.actors.TableSpaceActor;
import org.nustaq.reallive.server.storage.ClusterTableRecordMapping;
import org.nustaq.reallive.api.TableState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DynTableSpaceActor extends TableSpaceActor {

    public IPromise<Map<String,TableState>> getStates() {
        Promise p = new Promise();
        List<IPromise<TableState>> collect = tables.values().stream().map(en -> en.getTableState()).collect(Collectors.toList());
        all(collect).then( (r,e) -> {
            Map<String,TableState> res = new HashMap<>();
            collect.stream().map( prom -> prom.get() ).forEach( tableState -> res.put(tableState.getTableName(),tableState));
            p.resolve(res);
        });
        return p;
    }

    /**
     * forwards to table actors
     *
     * @param tableName
     * @param mapping
     * @return
     */
    public IPromise _setMapping(String tableName, ClusterTableRecordMapping mapping) {
        RealLiveTableActor realLiveTableActor = tables.get(tableName);
        if ( realLiveTableActor == null )
            return reject("table "+tableName+" not initialized");
        return realLiveTableActor._setMapping(mapping);
    }

}
