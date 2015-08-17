package org.nustaq.reallive.interfaces;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.reallive.messages.StateMessage;

import java.util.List;

/**
 * Created by ruedi on 08.08.2015.
 */
public interface TableSpace {

    /**
     * ndicates to use a default (or self determine) in case this is set as a base dir
     */
    public static final String USE_BASE_DIR = "USE_BASE_DIR";

    IPromise<RealLiveTable> createOrLoadTable(TableDescription desc);
    IPromise dropTable( String name );
    IPromise<List<TableDescription>> getTableDescriptions();
    IPromise<List<RealLiveTable>> getTables();
    IPromise<RealLiveTable> getTable(String name);
    IPromise shutDown();
    void stateListener( Callback<StateMessage> stateListener );

}
