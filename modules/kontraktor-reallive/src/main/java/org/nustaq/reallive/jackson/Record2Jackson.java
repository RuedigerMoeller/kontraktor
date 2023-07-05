package org.nustaq.reallive.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.apache.commons.lang.NotImplementedException;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.api.ChangeMessage;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.messages.UpdateMessage;
import org.nustaq.reallive.records.MapRecord;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class Record2Jackson {
    static Record2Jackson singleton = new Record2Jackson();

    public static Record2Jackson get() {
        return singleton;
    }

    ObjectMapper mapper = new ObjectMapper();

    public String toPrettyString(JsonNode node) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public ObjectNode fromRecord(Record r) {
        String key = r.getKey();
        ObjectNode res = mapper.createObjectNode();
        if (key != null) {
            res.put("key", key);
        }
        res.put("lastModified", r.getLastModified());
        String[] fields = r.getFields();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];
            Object value = r.get(field);
            res.put(field, fromJavaValue(value));
        }
        return res;
    }

    public JsonNode fromJavaValue(Object value) {
        if (value instanceof String) {
            return new TextNode((String) value);
        } else if (value instanceof Long) {
            return new LongNode(((Number) value).longValue());
        } else if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return new IntNode(((Number) value).intValue());
        } else if (value instanceof Number) {
            return new DoubleNode(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            return BooleanNode.valueOf(((Boolean) value).booleanValue());
        } else if (value instanceof Object[]) {
            ArrayNode jarr = mapper.createArrayNode();
            Object arr[] = (Object[]) value;
            for (int j = 0; j < arr.length; j++) {
                jarr.add(fromJavaValue(arr[j]));
            }
            return jarr;
        } else if (value instanceof Collection) {
            ArrayNode jarr = mapper.createArrayNode();
            ((Collection<?>) value).forEach(v -> jarr.add(fromJavaValue(v)));
            return jarr;
        } else if (value instanceof Record) {
            return fromRecord((Record) value);
        } else if (value == null) {
            return NullNode.getInstance();
        } else if (value instanceof JsonNode) {
            return (JsonNode) value;
        } else if (value instanceof Map) {
            throw new NotImplementedException();
        } else {
            if (value != null) {
                System.out.println("unmapped data " + value.getClass().getName());
            }
            System.out.println("unmapped data " + value);
        }
        return new TextNode("" + value);
    }

    public Record toRecord(ObjectNode members) {
        MapRecord aNew = MapRecord.New(null);
        Iterator<String> it = members.fieldNames();
        while (it.hasNext()) {
            String field = it.next();
            if ("key".equals(field)) {
                aNew.key(members.get(field).textValue());
                continue;
            }
            JsonNode jsonValue = members.get(field);
            if (jsonValue instanceof TextNode) {
                aNew.put(field, jsonValue.textValue());
            } else if (jsonValue.isNull()) {
                aNew.put(field, null);
            } else if (jsonValue.isNumber()) {
                aNew.put(field, fromJsonNumber(jsonValue));
            } else if (jsonValue.isBoolean()) {
                aNew.put(field, jsonValue.asBoolean());
            } else if (jsonValue.isObject()) {
                aNew.put(field, toRecord((ObjectNode) jsonValue));
            } else if (jsonValue.isArray()) {
                aNew.put(field, toRecordArray((ArrayNode) jsonValue));
            } else {
                throw new RuntimeException("unexpected json type:" + jsonValue.getClass());
            }
        }
        return aNew;
    }

    private Number fromJsonNumber(JsonNode val) {
        if (val instanceof IntNode) {
            return val.asInt();
        } else if (val instanceof DoubleNode) {
            return val.asDouble();
        }
        return val.asLong();
    }

    public Object[] toRecordArray(ArrayNode arr) {
        Object[] res = new Object[arr.size()];
        int i = 0;
        for (JsonNode jsonValue : arr) {
            if (jsonValue instanceof TextNode) {
                res[i] = jsonValue.textValue();
            } else if (jsonValue.isNull()) {
                res[i] = null;
            } else if (jsonValue.isNumber()) {
                res[i] = fromJsonNumber(jsonValue);
            } else if (jsonValue.isBoolean()) {
                res[i] = jsonValue.asBoolean();
            } else if (jsonValue.isObject()) {
                res[i] = toRecord((ObjectNode) jsonValue);
            } else if (jsonValue.isArray()) {
                res[i] = toRecordArray((ArrayNode) jsonValue);
            } else {
                throw new RuntimeException("unexpected json type:" + jsonValue.getClass());
            }
            i++;
        }
        return res;
    }

    public ObjectNode fromChange(ChangeMessage change) {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        switch (change.getType()) {
            case ChangeMessage.ADD: {
                result.put("type", "ADD");
                result.put("senderId", change.getSenderId());
                if (result.get("record") == null) {
                    result.set("record", fromRecord(change.getRecord()));
                }
                return result;
            }
            case ChangeMessage.REMOVE: {
                result.put("type", "REMOVE");
                result.put("senderId", change.getSenderId());
                if (result.get("record") == null) {
                    result.set("record", fromRecord(change.getRecord()));
                }
                return result;
            }
            case ChangeMessage.UPDATE: {
                result.put("type", "UPDATE");
                result.put("senderId", change.getSenderId());
                if (result.get("record") == null) {
                    result.set("record", fromRecord(change.getRecord()));
                }
                ObjectNode diff = JsonNodeFactory.instance.objectNode();
                UpdateMessage upd = (UpdateMessage) change;
                String[] changedFields = upd.getDiff().getChangedFields();
                Object[] oldValues = upd.getDiff().getOldValues();
                for (int i = 0; i < changedFields.length; i++) {
                    String changedField = changedFields[i];
                    if (diff != null) {
                        diff.set(changedField, fromJavaValue(oldValues[i]));
                    }

                }
                if (result.get("diff") == null) {
                    result.set("diff", diff);
                }

                return result;
            }
            case ChangeMessage.QUERYDONE:
                result.put("type", "QUERYDONE");
                return result;
            default:
                Log.Error(Record2Jackson.class, "unexpected change type");
        }
        return result;
    }


    public static void main(String[] args) throws IOException {


        String key = "9991";
        Record rec = Record.from(
            "key", key,
            "availabilityRules",
            Record.from("appointmentProviderClass",
                Record.from("rec",
                    Record.from("x", 1, "y", new Object[]{
                        1, 2L, 3, 4.2, Record.from("sub", "record"), "bla"
                    }))));

        String autMapped = get().mapper.writeValueAsString(rec);
        System.out.println("autoMapped:"+autMapped);

        Record autUnmapped = get().mapper.readValue(autMapped,Record.class);
        System.out.println("autoUnMapped:"+autUnmapped.toPrettyString());


//        System.out.println("record:" + rec.toPrettyString());
//        ObjectNode jsonNodes = get().fromRecord(rec);
//        System.out.println(get().toPrettyString(jsonNodes));
//
//        Record copied = get().toRecord(jsonNodes);
//        System.out.println("from ObjectNode to Record\n" + copied.toPrettyString());


    }


}
