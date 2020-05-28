package org.nustaq.reallive.server.dynamic;

import org.nustaq.reallive.server.dynamic.actions.ClusterTableAction;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.api.TableState;
import org.nustaq.reallive.server.storage.ClusterTableRecordMapping;

import java.io.Serializable;
import java.util.*;

/**
 * might contain state (actions)
 */
public class DynClusterTableDistribution implements Serializable {
    List<TableState> tableStates = new ArrayList<>();
    List<ClusterTableAction> actions;

    private String name;

    public DynClusterTableDistribution(String name) {
        this.name = name;
    }

    public List<TableState> getStates() {
        return tableStates;
    }

    public static final int OK = 0;
    public static final int INTERSECT = 1;
    public static final int INCOMPLETE = 2;
    public static final int EMPTY = 4;
    public static final int TABLE_MISSING = 8;

    public int sanitize() {
        List<TableState> l = tableStates;
        int res = 0;
        int numNodes = -1;
        BitSet bs = new BitSet(ClusterTableRecordMapping.NUM_BUCKET);
        for (int i = 0; i < l.size(); i++) {
            TableState tableState = l.get(i);
            if ( numNodes < 0 )
                numNodes = l.size();
            else if ( l.size() != numNodes ) {
                res |= TABLE_MISSING;
            }
            BitSet tableBS = tableState.getMapping().getBitset();
            if ( bs.intersects(tableBS) ) {
                Log.Warn(this, "data intersection "+tableState);
                res |= INTERSECT;
            }
            bs.or(tableBS);
        }
        if ( bs.nextClearBit(0) != ClusterTableRecordMapping.NUM_BUCKET )
            res |= INCOMPLETE;
        if ( bs.isEmpty() )
            res |= EMPTY;
        return res;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void add(TableState value) {
        tableStates.add(value);
    }

    public List<ClusterTableAction> getActions() {
        if ( actions == null )
            actions = new ArrayList<>();
        return actions;
    }

    public void addAction( ClusterTableAction action ) {
        getActions().add(action);
    }

    public void setActions(List<ClusterTableAction> actions) {
        this.actions = actions;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(100);
        s.append("=> "+getName()+"\n");
        tableStates.forEach(
            ts -> s.append(ts.getAssociatedShardName()+" "+ts.getMapping()+" "+ts.getNumElements()+"\n"));
        s.append("\n");
        return s.toString();
    }

    public void clearActions() {
        actions = null;
    }

    public boolean covers(int i) {
        for (int j = 0; j < tableStates.size(); j++) {
            TableState tableState = tableStates.get(j);
            if ( tableState.containsBucket(i) )
                return true;
        }
        return false;
    }
}
