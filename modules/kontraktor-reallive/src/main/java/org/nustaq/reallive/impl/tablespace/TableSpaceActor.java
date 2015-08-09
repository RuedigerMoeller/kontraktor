package org.nustaq.reallive.impl.tablespace;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.reallive.interfaces.RealLiveTable;
import org.nustaq.reallive.interfaces.TableDescription;
import org.nustaq.reallive.interfaces.TableSpace;
import org.nustaq.reallive.messages.StateMessage;

import java.util.HashMap;
import java.util.List;

/**
 * Created by ruedi on 08.08.2015.
 */
public class TableSpaceActor extends Actor<TableSpaceActor> implements TableSpace {

    HashMap<String,RealLiveTable> tables;

    @Local
    public void init() {

    }

    @Override
    public IPromise<RealLiveTable> createTable(TableDescription desc) {
        return null;
    }

    @Override
    public IPromise<List<TableDescription>> getTableDescriptions() {
        return null;
    }

    @Override
    public IPromise<List<RealLiveTable>> getTables() {
        return null;
    }

    @Override
    public IPromise shutDown() {
        return null;
    }

    @Override
    public void stateListener(Callback<StateMessage> stateListener) {

    }
}
