package org.nustaq.reallive.client;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.reallive.server.storage.StorageStats;
import org.nustaq.reallive.api.RealLiveTable;
import org.nustaq.reallive.api.TableDescription;
import org.nustaq.reallive.api.TableSpace;
import org.nustaq.reallive.messages.StateMessage;

import java.util.List;

/**
 * Makes an actor out of TableSpaceSharding by wrapping it. Runs client side.
 */
public class ClusteredTableSpaceClient<T extends ClusteredTableSpaceClient> extends Actor<T> implements TableSpace {

    protected TableSpaceSharding tableSpaceSharding;

    @Override
    public IPromise<RealLiveTable> createOrLoadTable(TableDescription desc) {
        return tableSpaceSharding.createOrLoadTable(desc);
    }

    @Override
    public IPromise dropTable(String name) {
        return tableSpaceSharding.dropTable(name);
    }

    @Override
    public IPromise<List<TableDescription>> getTableDescriptions() {
        return tableSpaceSharding.getTableDescriptions();
    }

    public IPromise<List<StorageStats>> getStats() {
        return resolve(tableSpaceSharding.getStats());
    }

    @Override
    public IPromise<List<RealLiveTable>> getTables() {
        return tableSpaceSharding.getTables();
    }

    @Override
    public IPromise<RealLiveTable> getTableAsync(String name) {
        return tableSpaceSharding.getTableAsync(name);
    }

    @Override
    public IPromise shutDown() {
        return tableSpaceSharding.shutDown();
    }

    @Override
    public void stateListener(Callback<StateMessage> stateListener) {
        tableSpaceSharding.stateListener(stateListener);
    }

}
