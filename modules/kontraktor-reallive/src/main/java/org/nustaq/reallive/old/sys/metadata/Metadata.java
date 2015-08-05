package org.nustaq.reallive.old.sys.metadata;

import org.nustaq.reallive.old.sys.config.ColumnConfig;
import org.nustaq.reallive.old.sys.config.ConfigReader;
import org.nustaq.reallive.old.sys.config.SchemaConfig;
import org.nustaq.reallive.old.sys.config.TableConfig;
import org.nustaq.serialization.FSTConfiguration;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ruedi on 10.07.2014.
 */
public class Metadata implements Serializable {

    String name;
    Map<String,TableMeta> tables = new HashMap<>();

    public TableMeta getTable(String name) {
        return tables.get(name);
    }

    public void putTable(String name, TableMeta meta) {
        tables.put(name,meta);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Metadata copyOverrideBy( String fileName ) {
        final Metadata metadata = FSTConfiguration.getDefaultConfiguration().deepCopy(this);
        try {
            if ( new File(fileName).exists() ) {
                SchemaConfig schemaConfig = ConfigReader.readConfig(fileName);
                metadata.overrideWith(schemaConfig);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return metadata;
    }

    public void overrideWith(SchemaConfig conf) {
        tables.keySet().forEach( (tableId) -> {
            TableConfig table = conf.getTable(tableId);
            if (table != null ) {
                tables.get(tableId).getColumns().keySet().forEach( (columnId) -> {
                    ColumnConfig config = table.getConfig(columnId);
                    final TableMeta tableMeta = tables.get(tableId);
                    final ColumnConfig globalCol = conf.getGlobals().get(columnId);
                    applyOverride(columnId, globalCol, tableMeta);
                    applyOverride(columnId, config, tableMeta);
                });
            } else {
                tables.get(tableId).getColumns().keySet().forEach( columnId -> {
                    final ColumnConfig globalCol = conf.getGlobals().get(columnId);
                    applyOverride(columnId, globalCol, tables.get(tableId));
                });
            }
        });
    }

    private void applyOverride(String columnId, ColumnConfig config, TableMeta tableMeta) {
        if ( config != null ) {
            ColumnMeta column = tableMeta.getColumn(columnId);

            if ( config.hidden != null ) column.setHidden(config.hidden);
            if ( config.colOrder != null ) column.setOrder(config.colOrder);
            if ( config.description != null ) column.setDescription(config.description);
            if ( config.displayName != null ) column.setDisplayName(config.displayName);
        }
    }
}
