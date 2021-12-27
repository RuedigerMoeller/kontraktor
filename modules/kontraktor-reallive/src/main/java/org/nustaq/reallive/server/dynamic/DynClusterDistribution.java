package org.nustaq.reallive.server.dynamic;

import com.eclipsesource.json.JsonObject;
import org.nustaq.reallive.api.TableState;
import org.nustaq.reallive.server.actors.RealLiveTableActor;

import java.io.Serializable;
import java.util.*;

public class DynClusterDistribution implements Serializable {
    Map<String,DynClusterTableDistribution> distributions = new HashMap<>(); // tablename to tabledist

    public void add(DynClusterTableDistribution ts) {
        if ( distributions.containsKey(ts.getName()))
            throw new RuntimeException("double distribution"+ts.getName());
        distributions.put(ts.getName(),ts);
    }

    public DynClusterTableDistribution have(String name) {
        DynClusterTableDistribution res = distributions.get(name);
        if ( res == null ) {
            res = new DynClusterTableDistribution(name);
            distributions.put(name,res);
        }
        return res;
    }

    public DynClusterTableDistribution get(String name) {
        return distributions.get(name);
    }

    public Collection<String> getTableNames() {
        return distributions.keySet();
    }

    public void clearActions() {
        distributions.values().forEach( tdist -> tdist.clearActions() );
    }

    public int getNumberOfShards() {
        if ( distributions == null )
            return 0;
        if ( distributions.isEmpty() )
            return 0;
        return distributions.values().iterator().next().getStates().size();
    }

    public Map<String, DynClusterTableDistribution> getDistributions() {
        return distributions;
    }

    public void setTableActor(String table, String shardName, RealLiveTableActor realLiveTableActor) {
        List<TableState> states = distributions.get(table).getStates();
        for (int i = 0; i < states.size(); i++) {
            TableState tableState = states.get(i);
            if ( tableState.getAssociatedShardName().equals(shardName) ) {
                tableState.associatedTableShard(realLiveTableActor);
            }
        }
    }

    public boolean hasFullCoverage() {
        for (Iterator<DynClusterTableDistribution> iterator = distributions.values().iterator(); iterator.hasNext(); ) {
            DynClusterTableDistribution dynClusterTableDistribution = iterator.next();
            if ( dynClusterTableDistribution.sanitize() != 0 ) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        StringBuilder res = new StringBuilder(200);
        distributions.forEach( (k,v) -> {
            res.append(v);
        });
        return res.toString();
    }

    public JsonObject toJsonObj() {
        JsonObject res = new JsonObject();
        distributions.forEach( (k,v) -> {
            res.set(k,v.toJsonObj());
        });
        return res;
    }

}
