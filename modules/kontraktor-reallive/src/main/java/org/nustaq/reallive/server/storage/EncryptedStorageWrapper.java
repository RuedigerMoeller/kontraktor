package org.nustaq.reallive.server.storage;

import org.nustaq.kontraktor.Spore;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.api.RecordStorage;
import org.nustaq.reallive.server.RemoveLog;

import java.util.function.Function;
import java.util.stream.Stream;

public class EncryptedStorageWrapper implements RecordStorage {

    Function<Record,Record> encryptionFun = r -> r;
    Function<Record,Record> decryptionFun = r -> r;
    RecordStorage wrapped;

    public EncryptedStorageWrapper(Function<Record, Record> encryptionFun, Function<Record, Record> decryptionFun, RecordStorage wrapped) {
        this.encryptionFun = encryptionFun;
        this.decryptionFun = decryptionFun;
        this.wrapped = wrapped;
    }

    @Override
    public RecordStorage put(String key, Record value) {
        return wrapped.put(key,encryptionFun.apply(value));
    }

    @Override
    public Record get(String key) {
        return decryptionFun.apply(wrapped.get(key));
    }

    @Override
    public Record remove(String key) {
        return decryptionFun.apply(wrapped.remove(key));
    }

    @Override
    public long size() {
        return wrapped.size();
    }

    @Override
    public StorageStats getStats() {
        return wrapped.getStats();
    }

    @Override
    public Stream<Record> stream() {
        return wrapped.stream().map( decryptionFun );
    }

    @Override
    public void resizeIfLoadFactorLarger(double loadFactor, long maxGrow) {
        wrapped.resizeIfLoadFactorLarger(loadFactor,maxGrow);
    }

    @Override
    public RemoveLog getRemoveLog() {
        return wrapped.getRemoveLog();
    }

    @Override
    public <T> void forEachWithSpore(Spore<Record, T> spore) {
        wrapped.forEachWithSpore(new Spore<Record, Object>() {
            @Override
            public void remote(Record input) {
                spore.remote(decryptionFun.apply(input));
            }
        });
    }
}
