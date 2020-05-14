package org.nustaq.kontraktor.services.rlserver.mongodb;

import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.reactivestreams.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.nustaq.kontraktor.Spore;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.api.TableDescription;
import org.nustaq.reallive.impl.storage.RecordPersistance;
import org.nustaq.reallive.impl.storage.StorageStats;

import java.util.concurrent.atomic.AtomicInteger;

import static com.mongodb.client.model.Filters.*;

public class MongoPersistance implements RecordPersistance {

    private final TableDescription description;
    MongoCollection collection;

    public MongoPersistance(MongoCollection col, TableDescription description) {
        collection = col;
        this.description = description;
    }

    @Override
    public Record remove(String key) {
        collection.deleteOne( eq("_id", new ObjectId(key) ) );
        return null;
    }

    @Override
    public StorageStats getStats() {
        return new StorageStats().name("mongo:"+description.getName());
    }

    @Override
    public <T> void forEachWithSpore(Spore<Record, T> spore) {
        AtomicInteger count = new AtomicInteger();
        long now = System.currentTimeMillis();
        collection.find().subscribe(new SubscriberHelpers.OperationSubscriber() {
            @Override
            public void onNext(Object o) {
                super.onNext(o);
                count.incrementAndGet();
                try {
                    spore.remote(MongoUtil.get().toRecord((Document)o));
                } catch ( Throwable ex ) {
                    Log.Warn(this, ex, "exception in spore " + spore);
                    throw ex;
                }
            }

            @Override
            public void onError(Throwable throwable) {
                spore.complete(null,throwable);
            }

            @Override
            public void onComplete() {
                Log.Info(this,""+description+" count forEach "+count+" time:"+(System.currentTimeMillis()-now));
                spore.finish();
            }
        });
    }

    @Override
    public RecordPersistance put(String key, Record record) {
        record.internal_setLastModified(System.currentTimeMillis());
        return _put(key,record);
    }

    ReplaceOptions upsert = new ReplaceOptions().upsert(true);
    @Override
    public RecordPersistance _put(String key, Record record) {
        record.key(key);
        Document replacement = MongoUtil.get().fromRecord(record);
        collection.replaceOne( eq("key", key), replacement, upsert)
        .subscribe(new SubscriberHelpers.OperationSubscriber() {
            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }
            @Override
            public void onComplete() {
//                System.out.println("completed");
            }
        });
        return this;
    }
}
