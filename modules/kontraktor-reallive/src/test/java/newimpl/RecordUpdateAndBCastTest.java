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

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class RecordUpdateAndBCastTest {
    @Test
    public void testBroadcastsOnJoin() throws InterruptedException {
        RealLiveTable table = createTable();
        Record test = createTestRecord();
        ChangeMessage broadCasts[] = new ChangeMessage[5];
        AtomicInteger msgIndex = new AtomicInteger();
        table.subscribeOn( r -> true, change -> {
            System.out.println(change);
            broadCasts[msgIndex.getAndIncrement()] = change;
        });
        while( broadCasts[0] == null ) Thread.yield();

        table.setRecord(test); // add message 1

        Record rec = table.get("someKey").await();
        Assert.assertTrue(rec.equals(test));

        table.join( Record.from(
            "key", "someKey",
            "y", Record.from(
                "a", 2
            )
        ));
        while( broadCasts[2] == null ) Thread.yield();
        UpdateMessage updateBroadcase = (UpdateMessage) broadCasts[2];
        Assert.assertTrue(2 == (Integer) updateBroadcase.getNewRecord().mget("y", "a"));
        Assert.assertTrue(1 == (Integer) updateBroadcase.getOldRecord().mget("y", "a"));

        Record copied = test.deepCopy();
        copied.getArr("z")[2] = 4;
        table.join(  copied );
        while( broadCasts[3] == null ) Thread.yield();
        updateBroadcase = (UpdateMessage) broadCasts[3];
        Assert.assertTrue( updateBroadcase.getDiff().getChangedFields()[0].equals("z"));

    }
    @Test
    public void testBroadcastsOnUpdateKV() throws InterruptedException {
        RealLiveTable table = createTable();
        ChangeMessage broadCasts[] = new ChangeMessage[3];
        AtomicInteger msgIndex = new AtomicInteger();
        table.subscribeOn( r -> true, change -> {
            System.out.println(change);
            broadCasts[msgIndex.getAndIncrement()] = change;
        });
        while( broadCasts[0] == null ) Thread.yield();
        Record test = createTestRecord();
        table.setRecord(test);
        while( broadCasts[1] == null ) Thread.yield();
        table.update(  "someKey", "x", "13" );
        while( broadCasts[2] == null ) Thread.yield();
        UpdateMessage updateBroadcase = (UpdateMessage) broadCasts[2];
        Assert.assertTrue( table.get("someKey").await().getString("x").equals("13"));
        Assert.assertTrue( updateBroadcase.getDiff().getChangedFields()[0].equals("x"));
    }

    private static Record createTestRecord() {
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
            "w", new Object[]{1, 2, 3},
            "z", new Object[]{1, 2, 3}
        );
        return test;
    }

    private static RealLiveTable createTable() {
        TableDescription desc = new TableDescription()
            .storageType(TableDescription.TEMP)
            .name("test");
        RealLiveTable table = EmbeddedRealLive.get().createTable(desc,"").await();
        return table;
    }

    @Test
    public void testPut() throws InterruptedException {
        RealLiveTable table = createTable();
        Record testRecord = createTestRecord();
        AtomicReference<UpdateMessage> msg = new AtomicReference<>();
        AtomicBoolean done = new AtomicBoolean(false);
        table.setRecord(testRecord);
        table.subscribeOn( "true", change -> {
            System.out.println("chg: "+change);
            if ( ! change.isDoneMsg() ) {
                if ( done.get() )
                    msg.set((UpdateMessage) change);
            } else
                done.set(true);
        });
        while ( ! done.get() ) Thread.yield();
        Record test = Record.from(
            "key","someKey",
            "x", 2, // diff in value
            "y", Record.from( //  no diff
                "a", 1,
                "b", "pokpok",
                "c", new Object[] {
                    "one", "two", "three"
                }
            ),
            "w", new Object[]{1, 2, 3} // unchanged
            // z missing
        );
        table.setRecord(test);
        while( msg.get() == null ) Thread.yield();
        HashSet<String> fields = new HashSet<>(List.of(msg.get().getDiff().getChangedFields()));
        Assert.assertTrue( fields.size() == 2 && fields.contains("z") && fields.contains("x") );
        System.out.println(msg.get());
    }

    @Test
    public void testUpdateRecord() throws InterruptedException {
        RealLiveTable table = createTable();
        Record testRecord = Record.from(
            "key","someKey",
            "x", 1,
            "y", Record.from(
                "a", 1,
                "b", "pokpok",
                "c", new Object[] {
                    "one", "two", "three"
                }
            ),
            "w", new Object[]{1, 2, 3},
            "z", new Object[]{1, 2, 3}
        );
        Record testUpdate = Record.from(
            "key","someKey",
            "x", 1, // no diff
            "y", Record.from( // diff
                "a", 1,
                "b", "pokpok",
                "c", new Object[] {
                    "one", "two", "three", "four"
                }
            ),
            "w", new Object[]{1, 2, 3, 4} // diff
            // z missing
        );
        AtomicReference<UpdateMessage> msg = new AtomicReference<>();
        AtomicBoolean done = new AtomicBoolean(false);
        table.setRecord(testRecord);
        table.subscribeOn( "true", change -> {
            System.out.println("chg: "+change);
            if ( ! change.isDoneMsg() ) {
                if ( done.get() )
                    msg.set((UpdateMessage) change);
            } else
                done.set(true);
        });
        while ( ! done.get() ) Thread.yield();
        table.upsertRecord(testUpdate);
        while( msg.get() == null ) Thread.yield();
        HashSet<String> fields = new HashSet<>(List.of(msg.get().getDiff().getChangedFields()));
        Assert.assertTrue( fields.size() == 3 && fields.contains("z") && fields.contains("y") && fields.contains("w") );
        System.out.println(msg.get());
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
    public void testAsLong() {
        Record rec = Record.from(
            "long", 1,
            "double", 1.0,
            "strLong", "1",
            "strDouble", "1.0"
        );
        Assert.assertTrue(rec.asLong("long") == 1 );
        Assert.assertTrue(rec.asLong("double") == 1 );
        Assert.assertTrue(rec.asLong("strLong") == 1 );
        Assert.assertTrue(rec.asLong("strDouble") == 1 );

        Assert.assertTrue(rec.asDouble("long") == 1.0 );
        Assert.assertTrue(rec.asDouble("double") == 1.0 );
        Assert.assertTrue(rec.asDouble("strLong") == 1.0 );
        Assert.assertTrue(rec.asDouble("strDouble") == 1.0 );
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
