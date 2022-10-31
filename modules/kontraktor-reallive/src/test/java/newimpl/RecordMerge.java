package newimpl;

import com.eclipsesource.json.Json;
import org.junit.Assert;
import org.junit.Test;
import org.nustaq.reallive.api.ChangeMessage;
import org.nustaq.reallive.api.RealLiveTable;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.api.TableDescription;
import org.nustaq.reallive.client.EmbeddedRealLive;
import org.nustaq.reallive.messages.UpdateMessage;

import java.util.concurrent.atomic.AtomicInteger;

public class RecordMerge {
    @Test
    public void testBroadcastsOnJoin() throws InterruptedException {
        TableDescription desc = new TableDescription()
            .storageType(TableDescription.TEMP)
            .name("test");
        RealLiveTable table = EmbeddedRealLive.get().createTable(desc,"").await();
        Record test = Record.from(
            "key","someKey",
            "x", 1,
            "y", Record.from(
                "a", 1,
                "b", "pokpok",
                "c", new Object[] {
                    "one", "two", "three"
                }
            ),
            "z", new Object[]{1, 2, 3}
        );
        table.setRecord(test);


        Record rec = table.get("someKey").await();
        Assert.assertTrue(rec.equals(test));

        ChangeMessage broadCasts[] = new ChangeMessage[4];
        AtomicInteger msgIndex = new AtomicInteger();
        table.subscribeOn( r -> true, change -> {
            System.out.println(change);
            broadCasts[msgIndex.getAndIncrement()] = change;
        });
        Thread.sleep(1000);
        table.join( Record.from(
            "key", "someKey",
            "y", Record.from(
                "a", 2
            )
        ));
        Thread.sleep(1000);
        UpdateMessage updateBroadcase = (UpdateMessage) broadCasts[2];
        Assert.assertTrue(2 == (Integer) updateBroadcase.getNewRecord().mget("y", "a"));
        Assert.assertTrue(1 == (Integer) updateBroadcase.getOldRecord().mget("y", "a"));

        table.update(  "someKey", "x", "13" );
        Thread.sleep(1000);
        updateBroadcase = (UpdateMessage) broadCasts[3];
        Assert.assertTrue( table.get("someKey").await().getString("x").equals("13"));
        Assert.assertTrue( updateBroadcase.getDiff().getChangedFields()[0].equals("x"));
    }

    String expected = "{\n" +
        "  \"xx\": \"Hello\",\n" +
        "  \"yy\": {\n" +
        "    \"c\": 13,\n" +
        "    \"b\": 99\n" +
        "  },\n" +
        "  \"x\": 2,\n" +
        "  \"z\": [\n" +
        "    1,\n" +
        "    2,\n" +
        "    3,\n" +
        "    4\n" +
        "  ],\n" +
        "  \"y\": {\n" +
        "    \"c\": 13,\n" +
        "    \"b\": 99,\n" +
        "    \"a\": 1\n" +
        "  },\n" +
        "  \"zz\": [\n" +
        "    1,\n" +
        "    4\n" +
        "  ]\n" +
        "}";
    @Test
    public void testJoin() {
        Record target = Record.from(
            "x", 1,
            "y", Record.from("a", 1, "b", 2),
            "z", new Object[]{1, 2, 3}
        );
        target.join(
            Record.from(
                "x", 2, // overwrite
                "xx", "Hello", // new attribute
                "y", Record.from("b", 99, "c", 13), // join nested
                "z", new Object[]{1, 4}, // join arrays
                "yy", Record.from("b", 99, "c", 13), // new subrecord
                "zz", new Object[]{1, 4} // new array
            )
        );
        Record verify = Record.from(Json.parse(expected).asObject());
        Assert.assertTrue(target.defaultEquals(verify));
    }
    @Test
    public void testBaseMerge() {
        Record rec = Record.from(
            "key", "xy13key",
            "bl", true
        );
        rec.deepMerge(Record.from(
            "xy", "test"
        ));
        Assert.assertTrue(rec.get("xy").equals("test"));
        rec.deepMerge(Record.from(
            "xy", null
        ));
        Assert.assertTrue(rec.get("xy") == null);
    }

    @Test
    public void testRecordMerge() {
        Record rec = Record.from(
            "key", "xy13key",
            "sub", Record.from(
                "x", 1,
                "y", 2
            )
        );

        rec.deepMerge(Record.from(
            "sub+", Record.from( "test", 13 )
        ));
        Assert.assertTrue( rec.mget("sub", "test").equals(13) && rec.mget("sub", "x").equals(1) );

        rec.deepMerge(Record.from(
            "sub", Record.from( "test", 13 )
        ));
        Assert.assertTrue( rec.mget("sub", "test").equals(13) && rec.mget("sub", "x") == null );

        rec.deepMerge(Record.from(
            "sub", "test"
        ));
        Assert.assertTrue( "test".equals( rec.get("sub") ) );
    }

}
