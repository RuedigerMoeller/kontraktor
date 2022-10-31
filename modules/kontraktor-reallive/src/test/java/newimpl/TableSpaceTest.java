package newimpl;

import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.reallive.server.actors.TableSpaceActor;
import org.nustaq.reallive.client.TableSpaceSharding;
import org.nustaq.reallive.api.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.nustaq.reallive.api.Record;

/**
 * Created by ruedi on 14.08.2015.
 *
 * NOTE: this is not intended to be run automatically (needs manual server start)
 */
@Ignore
public class TableSpaceTest {

    public static final int EXPECT_SIMPLECOUNT = 603;

    @Test
    public void simple() {
        TableSpaceActor ts = Actors.AsActor(TableSpaceActor.class);
        ts.init();
        Assert.assertTrue( runSimpleTest(ts, () -> createTableDescription() ) == EXPECT_SIMPLECOUNT );
        ts.shutDown().await();
    }

    @Test
    public void simpleSharded() {
        TableSpaceActor spaces[] = {
            Actors.AsActor(TableSpaceActor.class),
            Actors.AsActor(TableSpaceActor.class)
        };
        for (int i = 0; i < spaces.length; i++) {
            TableSpaceActor space = spaces[i];
            space.init();
        }
        TableSpaceSharding ts = new TableSpaceSharding( spaces );
        Assert.assertTrue(runSimpleTest(ts, () -> createShardedTableDescription()) == EXPECT_SIMPLECOUNT);
        ts.shutDown().await();
    }

    @Test
    public void simpleShardedNoServer() {
        TableSpaceActor spaces[] = {
            null,
            null
        };
        spaces[0] =
        (TableSpaceActor) new TCPConnectable(TableSpaceActor.class, "localhost", 5432)
            .connect((disc, err) -> System.out.println("client disc " + disc + " " + err))
            .await();
        spaces[1] =
        (TableSpaceActor) new TCPConnectable(TableSpaceActor.class, "localhost", 5433)
            .connect((disc, err) -> System.out.println("client disc " + disc + " " + err))
            .await();
       for (int i = 0; i < spaces.length; i++) {
            TableSpaceActor space = spaces[i];
            space.init();
        }
        TableSpaceSharding ts = new TableSpaceSharding( spaces );
        Assert.assertTrue(runSimpleTest(ts, () -> createShardedTableDescription()) == EXPECT_SIMPLECOUNT);
        ts.shutDown().await();
    }

    @Test
    public void simpleRemote() {
        TableSpaceActor ts = startServer();

        Assert.assertTrue(runSimpleTest(ts, () -> createTableDescription()) == EXPECT_SIMPLECOUNT);

        ts.close();
        ts.shutDown().await();
    }

    @Test
    public void simpleRemoteNoServer() {
        TableSpaceActor remoteTS =
        (TableSpaceActor) new TCPConnectable(TableSpaceActor.class, "localhost", 5432)
            .connect((disc, err) -> System.out.println("client disc " + disc + " " + err))
            .await();

        Assert.assertTrue(runSimpleTest(remoteTS, () -> createTableDescription()) == EXPECT_SIMPLECOUNT);
    }

    @Test
    public void startServerNoResult() throws InterruptedException {
        startServer();
        Thread.sleep(1000000);
    }

    public TableSpaceActor startServer() {
        TableSpaceActor ts = Actors.AsActor(TableSpaceActor.class);
        ts.init();
        new TCPNIOPublisher(ts,5432).publish(actor -> System.out.println("sidconnected: " + actor));
        return ts;
    }

    @Test
    public void startShardServer() throws InterruptedException {
        startServer();
        TableSpaceActor ts = Actors.AsActor(TableSpaceActor.class);
        ts.init();
        new TCPNIOPublisher(ts,5433).publish(actor -> System.out.println("sidconnected: " + actor));
        Thread.sleep(1000000);
    }

    protected int runSimpleTest(TableSpace ts, Supplier<TableDescription> fac) {
        AtomicInteger resultCount = new AtomicInteger(0);
        if ( ts.getTableAsync("Test").await() == null ) {
            TableDescription test = fac.get();
            ts.createOrLoadTable(test).await();
        }
        RealLiveTable test = ts.getTableAsync("Test").await();

        test.forEach(rec -> true, (r, e) -> System.out.println("filter:" + r + " " + resultCount.incrementAndGet()));

        IntStream.range(0,100).forEach(i -> {
            test.upsert( "emöil" + i, "age", 9, "name", "Emil");
            test.upsert( "fölix" + i, "age", 17, "name", "Felix");
        });

        System.out.println(test.get("emil0").await());

        test.atomic("emöil0", rec -> ((Record)rec).getInt("age")).then(i -> System.out.println("atomicQuery age "+i));

        test.forEach(rec -> true, (r, e) -> System.out.println("filter1:" + r + " " + resultCount.incrementAndGet()));
        Subscriber subs[] = {null};
        subs[0] = new Subscriber(record -> true, change -> {
            System.out.println("stream: " + change + " " + resultCount.incrementAndGet());
            if (change.isDoneMsg()) {
                test.unsubscribe(subs[0]);
            }
        });
        test.subscribe(subs[0]);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return resultCount.get();
    }

    private TableDescription createTableDescription() {
        return new TableDescription("Test")
            .filePath("/tmp/test")
            .numEntries(500_000)
            .sizeMB(500)
            .shardNo(0);
    }

    private TableDescription createShardedTableDescription() {
        return new TableDescription("Test")
            .filePath("/tmp/testshard")
            .numEntries(500_000/2)
            .sizeMB(500/2)
            .shardNo(0);
    }

}
