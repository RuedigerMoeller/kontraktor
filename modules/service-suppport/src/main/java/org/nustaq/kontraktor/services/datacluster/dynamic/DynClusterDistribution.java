package org.nustaq.kontraktor.services.datacluster.dynamic;

import org.nustaq.reallive.api.TableState;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynClusterDistribution implements Serializable {
    Map<String,DynClusterTableDistribution> distributions = new HashMap<>();

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

    public Collection<String> getTableNames() {
        return distributions.keySet();
    }

    public void clearActions() {
        distributions.values().forEach( tdist -> tdist.clearActions() );
    }
}
