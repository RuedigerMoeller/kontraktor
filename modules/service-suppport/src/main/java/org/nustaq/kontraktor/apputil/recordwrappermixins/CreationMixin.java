package org.nustaq.kontraktor.apputil.recordwrappermixins;

import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.RecordWrapper;

public interface CreationMixin<T extends RecordWrapper> extends Record {

    default long getCreation() {
        return getLong("creation");
    }

    default T creation(final long creation) {
        this.put("creation", creation);
        return (T) this;
    }

}
