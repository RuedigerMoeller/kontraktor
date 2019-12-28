package org.nustaq.reallive.query;

import java.io.Serializable;

public class QToken implements Serializable {

    String value;
    String query;
    int pos;

    public String getValue() {
        return value;
    }

    public QToken(String value, String query, int pos) {
        this.value = value;
        this.query = query;
        this.pos = pos;
    }

    public String getQuery() {
        return query;
    }

    public int getPos() {
        return pos;
    }

    public String toErrorString() {
        String posEmpty = "";
        for ( int i = 0; i < pos; i++ )
            posEmpty+=" ";
        return "\n"+query+"\n"+posEmpty+"=====";
    }

    public String toString() {
        return value;
    }
}
