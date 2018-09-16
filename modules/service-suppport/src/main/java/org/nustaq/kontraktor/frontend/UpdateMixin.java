package org.nustaq.kontraktor.frontend;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.reallive.api.RealLiveTable;

public interface UpdateMixin {

    RealLiveTable getTable(String name);

    /**
     * update or add
     * @param table
     * @param key
     * @param keyVals
     */
    default IPromise updateRec(String table, String key, Object[] keyVals) {
        if ( keyVals.getClass() != Object[].class ) {
            Object tmp[] = new Object[keyVals.length];
            for (int i = 0; i < tmp.length; i++) {
                tmp[i] = keyVals[i];
            }
            keyVals = tmp;
        }
        RealLiveTable tab = getTable(table);
        if ( tab == null )
            return Actors.reject("table not found "+table);
        tab.update( key,keyVals);
        return Actors.resolve();
    }

    default IPromise removeRec(String table, String key) {
        RealLiveTable tab = getTable(table);
        if ( tab == null )
            return Actors.reject("table not found "+table);
        tab.remove( key);
        return Actors.resolve();
    }

}
