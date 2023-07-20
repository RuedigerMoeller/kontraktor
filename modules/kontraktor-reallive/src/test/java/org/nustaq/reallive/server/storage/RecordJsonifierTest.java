package org.nustaq.reallive.server.storage;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import org.junit.Test;
import org.nustaq.reallive.api.Record;

import java.util.Arrays;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class RecordJsonifierTest {

    public static final RecordJsonifier RECORD_JSONIFIER = RecordJsonifier.get();

    @Test
    public void testFromRecordHappyPath() {
        final String jsonString = "{" +
                "  \"key\": \"42\"," +
                "  \"obj\": {" +
                "    \"bool\": true," +
                "    \"str\": \"str\"," +
                "    \"number\": 1," +
                "    \"key\": \"43\"" +
                "  }," +
                "  \"arr\": [" +
                "    1," +
                "    true," +
                "    \"str\"" +
                "  ]" +
                "}";

        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        final Record record = RECORD_JSONIFIER.toRecord(jsonObject);

        assertEquals(record.getKey(), "42");

        final Record obj = record.getRec("obj");
        assertTrue(obj.getBool("bool"));
        assertEquals("str", obj.getString("str"));
        assertEquals(1, obj.getInt("number"));
        assertEquals("43", obj.getKey());

        final Object[] arr = record.getArr("arr");
        assertTrue(Arrays.equals(arr, new Object[]{1, true, "str"}));
    }

    @Test
    public void testFromRecordWithKeyNull() {
        final String jsonString = "{" +
                "  \"key\": \"42\"," +
                "  \"obj\": {" +
                "    \"key\": null," + // null here
                "    \"number\": 1" +
                "  }," +
                "  \"obj2\": {" +
                "    \"key\": 2," + // not a string here
                "    \"number\": 2" +
                "  }" +
                "}";

        final JsonObject jsonObject = Json.parse(jsonString).asObject();
        final Record record = RECORD_JSONIFIER.toRecord(jsonObject);

        assertEquals(record.getKey(), "42");
        assertEquals(null, record.getRec("obj").getKey());
        assertEquals(null, record.getRec("obj2").getKey());
    }
}