package org.nustaq.reallive.sys.tables;

import org.nustaq.reallive.Record;
import org.nustaq.reallive.sys.annotations.ColOrder;
import org.nustaq.reallive.sys.annotations.Description;
import org.nustaq.reallive.sys.metadata.TableMeta;

/**
 * Created by ruedi on 07.07.14.
 */
@Description("Table containing rows for each table of RealLive")
public class SysTable extends Record {

    @ColOrder(0)
    String tableName;
    @ColOrder(10)
    String description;
    TableMeta meta;

    @ColOrder(20)
    int sizeMB;
    @ColOrder(30)
    int freeMB;
    @ColOrder(15)
    int numElems;

    public TableMeta getMeta() {
        return meta;
    }

    public void setMeta(TableMeta meta) {
        this.meta = meta;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getSizeMB() {
        return sizeMB;
    }

    public void setSizeMB(int sizeMB) {
        this.sizeMB = sizeMB;
    }

    public int getFreeMB() {
        return freeMB;
    }

    public void setFreeMB(int freeMB) {
        this.freeMB = freeMB;
    }

    public int getNumElems() {
        return numElems;
    }

    public void setNumElems(int numElems) {
        this.numElems = numElems;
    }
}
