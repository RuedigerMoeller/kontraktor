package org.nustaq.reallive.sys.config;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by ruedi on 03.08.14.
 */
public class TableConfig implements Serializable {

    HashMap<String,ColumnConfig> columns;

    public ColumnConfig getConfig(String column) {
        return columns.get(column);
    }

    public HashMap<String, ColumnConfig> getColumns() {
        if ( columns == null )
            columns = new HashMap<>();
        return columns;
    }

}
