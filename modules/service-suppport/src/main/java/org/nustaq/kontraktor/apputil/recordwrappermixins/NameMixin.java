package org.nustaq.kontraktor.apputil.recordwrappermixins;

import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.RecordWrapper;

public interface NameMixin<T extends RecordWrapper> extends Record {

    default String getName() {
        return getString("name");
    }

    default T name(final String text) {
        this.put("name", text);
        return (T) this;
    }

}
