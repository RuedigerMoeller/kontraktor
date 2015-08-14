package newimpl;

import junit.framework.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.reallive.impl.actors.TableSharding;
import org.nustaq.reallive.impl.tablespace.TableSpaceActor;
import org.nustaq.reallive.impl.tablespace.TableSpaceSharding;
import org.nustaq.reallive.interfaces.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Created by ruedi on 14.08.2015.
 */
public class TableSpaceTest {

    public static final int EXPECT_SIMPLECOUNT = 603;

    @Test
    public void simple() {
        TableSpaceActor ts = Actors.AsActor(TableSpaceActor.class);
        ts.init(4, 0);
        Assert.assertTrue( runSimpleTest(ts) == EXPECT_SIMPLECOUNT );
        ts.shutDown().await();
    }

    @Test
    public void simpleSharded() {
        TableSpaceActor spaces[] = {
            Actors.AsActor(TableSpaceActor.class),
            Actors.AsActor(TableSpaceActor.class)
        };
        TableSpaceSharding ts = new TableSpaceSharding( spaces, key -> key.hashCode() );
        Assert.assertTrue(runSimpleTest(ts) == EXPECT_SIMPLECOUNT);
        ts.shutDown().await();
    }

    @Test
    public void simpleRemote() {
        TableSpaceActor ts = startServer();

        Assert.assertTrue(runSimpleTest(ts) == EXPECT_SIMPLECOUNT);

        ts.close();
        ts.shutDown().await();
    }

    @Test
    public void simpleRemoteNoServer() {TableSpaceActor remoteTS =
        (TableSpaceActor) new TCPConnectable(TableSpaceActor.class, "localhost", 5432)
            .connect((disc, err) -> System.out.println("client disc " + disc + " " + err))
            .await();

        Assert.assertTrue(runSimpleTest(remoteTS) == 9);
    }

    @Test
    public void startServerNoResult() throws InterruptedException {
        startServer();
        Thread.sleep(1000000);
    }

    public TableSpaceActor startServer() {
        TableSpaceActor ts = Actors.AsActor(TableSpaceActor.class);
        ts.init(4, 0);
        new TCPNIOPublisher(ts,5432).publish(actor -> System.out.println("sidconnected: " + actor));
        return ts;
    }

    protected int runSimpleTest(TableSpace ts) {
        AtomicInteger resultCount = new AtomicInteger(0);
        if ( ts.getTable("Test").await() == null ) {
            TableDescription test =
                new TableDescription("Test")
                    .filePath("/tmp/test")
                    .numEntries(500_000)
                    .sizeMB(500)
                    .shardNo(0);
            ts.createTable(test).await();
        }
        RealLiveTable test = ts.getTable("Test").await();

        test.filter(rec -> true, (r, e) -> System.out.println("filter:" + r + " " + resultCount.incrementAndGet()));

        Mutation mutation = test.getMutation();
        IntStream.range(0,100).forEach( i -> {
            mutation.addOrUpdate("emöil"+i, "age", 9, "name", "Emil");
            mutation.addOrUpdate("fölix"+i, "age", 17, "name", "Felix");
        });

        System.out.println(test.get("emil0").await());
        test.filter(rec -> true, (r, e) -> System.out.println("filter1:" + r + " " + resultCount.incrementAndGet()));
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
}
