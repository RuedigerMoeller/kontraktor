package org.nustaq.reallive.interfaces;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.reallive.messages.StateMessage;

import java.util.List;

/**
 * Created by ruedi on 08.08.2015.
 */
public interface TableSpace {

    IPromise<RealLiveTable> createTable( TableDescription desc );
    IPromise dropTable( String name );
    IPromise<List<TableDescription>> getTableDescriptions();
    IPromise<List<RealLiveTable>> getTables();
    IPromise<RealLiveTable> getTable(String name);
    IPromise shutDown();
    void stateListener( Callback<StateMessage> stateListener );

}
