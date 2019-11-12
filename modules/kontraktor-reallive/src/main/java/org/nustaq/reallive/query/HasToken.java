package org.nustaq.reallive.query;

public interface HasToken {
    QToken getToken();
    default String getErrorString() {
        if ( getToken() == null )
            return "no token:"+this;
        return getToken().toErrorString();
    }
}
