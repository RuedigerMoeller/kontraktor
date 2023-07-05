package org.nustaq.reallive.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.nustaq.reallive.api.Record;

import java.io.IOException;

public class RecordSerializer extends JsonSerializer<Record> {
    static ObjectMapper mapper = new ObjectMapper();

    @Override
    public void serialize(Record record, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        mapper.writeTree(jsonGenerator, Record2Jackson.get().fromRecord(record));
    }
}
