package org.nustaq.kontraktor.apputil.recordwrappermixins;

import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.RecordWrapper;

public interface LastModifiedMixin<T extends RecordWrapper> extends Record {

    default long getLastModified() {
        return getLong("lastModified");
    }

    default T lastModified(final long creation) {
        this.put("lastModified", creation);
        return (T) this;
    }

}
