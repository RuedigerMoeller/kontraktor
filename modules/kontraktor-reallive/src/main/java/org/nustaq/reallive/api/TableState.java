package org.nustaq.reallive.api;
import org.nustaq.reallive.server.storage.ClusterTableRecordMapping;
import java.io.Serializable;

public class TableState implements Serializable {

    Object associatedShard; // service description, cannot ref from here
    String tableName;
    ClusterTableRecordMapping mapping;
    long numElements;

    public TableState(ClusterTableRecordMapping mapping, long numElements, String name) {
        this.mapping = mapping;
        this.numElements = numElements;
        this.tableName = name;
    }

    /**
     * @return servicedescription
     */
    public Object getAssociatedShard() {
        return associatedShard;
    }

    public ClusterTableRecordMapping getMapping() {
        return mapping;
    }

    public long getNumElements() {
        return numElements;
    }

    public String getTableName() {
        return tableName;
    }

    public TableState associatedShard(Object assiatedShard) {
        this.associatedShard = assiatedShard;
        return this;
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
            ", assShard="+associatedShard+
            '}';
    }


}
