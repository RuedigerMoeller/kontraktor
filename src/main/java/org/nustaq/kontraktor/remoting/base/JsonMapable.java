package org.nustaq.kontraktor.remoting.base;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * marker interface. Signals this is a DTO which should be mapped from JSon if applies
 */
public interface JsonMapable {
    ObjectMapper mapper = ConnectionRegistry.CreateDefaultObjectMapper.get();
    default String toJsonString() {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    default JsonObject toJson() {
        try {
            return Json.parse(mapper.writeValueAsString(this)).asObject();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
