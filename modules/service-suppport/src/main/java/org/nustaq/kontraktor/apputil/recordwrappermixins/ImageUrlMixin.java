package org.nustaq.kontraktor.apputil.recordwrappermixins;

import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.RecordWrapper;

public interface ImageUrlMixin<T extends RecordWrapper> extends Record {

    default String getImageURL() {
        return getString("imageURL");
    }

    default T imageURL(final String text) {
        this.put("imageURL", text);
        return (T) this;
    }

}

