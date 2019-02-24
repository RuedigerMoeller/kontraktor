package org.nustaq.kontraktor.apputil.recordwrappermixins;

import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.RecordWrapper;

public interface TextMixin<T extends RecordWrapper> extends Record {

    default String getText() {
        return getString("text");
    }

    default T text(final String text) {
        this.put("text", text);
        return (T) this;
    }

}
