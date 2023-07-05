package org.nustaq.reallive.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.nustaq.reallive.api.Record;
import java.io.IOException;

public class RecordDeserializer extends JsonDeserializer<Record> {

    @Override
    public Record deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectNode node = Record2Jackson.get().mapper.readTree(p);
        return Record2Jackson.get().toRecord(node);
    }

}
