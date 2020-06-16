package org.nustaq.reallive.api;
import org.nustaq.reallive.server.actors.RealLiveTableActor;
import org.nustaq.reallive.server.storage.ClusterTableRecordMapping;
import java.io.Serializable;
import java.util.Arrays;
import java.util.BitSet;

public class TableState implements Serializable {

    transient RealLiveTableActor associatedTableShard; // service description, cannot ref from here
    String associatedShardName; // dependency terror
    String tableName;
    ClusterTableRecordMapping mapping;
    long numElements;

    public TableState(ClusterTableRecordMapping mapping, long numElements, String name) {
        this.mapping = mapping;
        this.numElements = numElements;
        this.tableName = name;
    }

    public RealLiveTableActor getAssociatedTableShard() {
        return associatedTableShard;
    }

    public ClusterTableRecordMapping getMapping() {
        return mapping;
    }

    public int getNumBuckets() {
        return getMapping().getBitset().cardinality();
    }

    public long getNumElements() {
        return numElements;
    }

    public String getTableName() {
        return tableName;
    }

    public TableState associatedTableShard(RealLiveTableActor assiatedShard ) {
        this.associatedTableShard = assiatedShard;
        return this;
    }

    public TableState associatedShardName(String name ) {
        this.associatedShardName = name;
        return this;
    }

    public String getAssociatedShardName() {
        return associatedShardName;
    }

    public TableState mapping(ClusterTableRecordMapping mapping) {
        this.mapping = mapping;
        return this;
    }

    public TableState numElements(long numElements) {
        this.numElements = numElements;
        return this;
    }

    public TableState tableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    @Override
    public String toString() {
        return "TableState{" +
            "tableName='" + tableName + '\'' +
            ", mapping=" + mapping +
            ", numElements=" + numElements +
            ", assShardName="+ associatedShardName +
            '}';
    }


    public boolean containsBucket(int i) {
        return mapping.getBitset().get(i);
    }

    public void addBuckets(int [] transfer ) {
        getMapping().addBuckets(transfer);
    }

    // FIXME: randomize buckets
    public int[] takeBuckets(int transfer) {
        int[] res = new int[transfer];
        BitSet bitset = getMapping().getBitset();
        int bitPos = 0;
        int emptyCount = 0;
        for (int i = 0; i < res.length; i++) {
            res[i] = bitset.nextSetBit(bitPos);
            if ( res[i] < 0 ) {
                emptyCount++;
            } else {
                bitPos = res[i];
                bitset.set(bitPos, false);
            }
        }
        if ( emptyCount > 0 ) {
            int[] newRes = new int[res.length-emptyCount];
            for (int i = 0; i < newRes.length; i++) {
                newRes[i] = res[i];
            }
            return newRes;
        }
        return res;
    }
}
