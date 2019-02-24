package org.nustaq.kontraktor.apputil.recordwrappermixins;

import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.RecordWrapper;

public interface IdMixin<T extends RecordWrapper> extends Record {

    default String getId() {
        return getString("id");
    }

    default T id(final String creation) {
        this.put("id", creation);
        return (T) this;
    }

}
