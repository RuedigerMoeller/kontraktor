package org.nustaq.kontraktor.services.datacluster.dynamic;

import org.nustaq.kontraktor.services.ServiceDescription;
import org.nustaq.kontraktor.services.datacluster.dynamic.actions.AssignMappingAction;
import org.nustaq.kontraktor.services.datacluster.dynamic.actions.ClusterTableAction;
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
    int sanitize() {
        List<TableState> l = tableStates;
        int res = 0;
        BitSet bs = new BitSet(ClusterTableRecordMapping.NUM_BUCKET);
        for (int i = 0; i < l.size(); i++) {
            TableState tableState = l.get(i);
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

    public void initFromEmpty() {
        int numNodes = tableStates.size();
        int tsCount = 0;
        for (int i = 0; i < ClusterTableRecordMapping.NUM_BUCKET; i++ ) {
            TableState tableState = tableStates.get(tsCount++);
            tableState.getMapping().setBucket(i,true);
            if ( tsCount >= numNodes )
                tsCount = 0;
        }
        setActions(new ArrayList<>());
        tableStates.forEach( tstate -> getActions().add(
            new AssignMappingAction(
                tstate.getTableName(),
                ((ServiceDescription)tstate.getAssociatedShard()).getName(),
                tstate.getMapping()
            )
        ));
    }

    public List<ClusterTableAction> getActions() {
        if ( actions == null )
            return Collections.emptyList();
        return actions;
    }

    public void setActions(List<ClusterTableAction> actions) {
        this.actions = actions;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(100);
        s.append("=> "+getName()+"\n");
        tableStates.forEach(
            ts -> s.append(((ServiceDescription) ts.getAssociatedShard()).getName()+" "+ts.getMapping()+" "+ts.getNumElements()+"\n"));
        s.append("\n");
        return s.toString();
    }

    public void clearActions() {
        actions = null;
    }
}
