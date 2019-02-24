package org.nustaq.kontraktor.apputil.recordwrappermixins;

import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.RecordWrapper;

public interface TypeMixin<T extends RecordWrapper> extends Record {

    default String getType() {
        return getString("type");
    }

    default T type(final String text) {
        this.put("type", text);
        return (T) this;
    }

}
