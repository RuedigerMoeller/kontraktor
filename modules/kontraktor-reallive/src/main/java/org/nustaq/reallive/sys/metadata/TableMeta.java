package org.nustaq.reallive.sys.metadata;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by ruedi on 09.07.2014.
 */
public class TableMeta implements Serializable {
    String name;
    String displayName;
    HashMap<String, ColumnMeta> columns = new HashMap<>();
    String customMeta;
    String description;

    public TableMeta() {
    }

    public TableMeta(String name, String displayName, ColumnMeta[] columns) {
        this.name = name;
        this.displayName = displayName;
        for (int i = 0; i < columns.length; i++) {
            ColumnMeta column = columns[i];
            this.columns.put(column.getName(),column);
        }
    }

    public HashMap<String, ColumnMeta> getColumns() {
        return columns;
    }

    public String getCustomMeta() {
        return customMeta;
    }

    public void setCustomMeta(String customMeta) {
        this.customMeta = customMeta;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ColumnMeta getColumn(String name) {
        return columns.get(name);
    }

    public void putColumn(String name, ColumnMeta cm) {
        columns.put(name, cm);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
