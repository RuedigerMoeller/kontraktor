package org.nustaq.reallive.sys.messages;

/**
 * Created by ruedi on 13.07.14.
 */

import java.io.Serializable;

/**
 * used for JS queries
 */
public class QueryTuple implements Serializable {
    String tableName;
    String querySource;

    public QueryTuple() {
    }

    public QueryTuple(String tableName, String querySource) {
        this.tableName = tableName;
        this.querySource = querySource;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getQuerySource() {
        return querySource;
    }

    public void setQuerySource(String querySource) {
        this.querySource = querySource;
    }
}
