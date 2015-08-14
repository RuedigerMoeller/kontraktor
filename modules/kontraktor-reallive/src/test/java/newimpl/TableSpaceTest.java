package newimpl;

import org.junit.Test;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.reallive.impl.tablespace.TableSpaceActor;
import org.nustaq.reallive.interfaces.Mutation;
import org.nustaq.reallive.interfaces.RealLiveTable;
import org.nustaq.reallive.interfaces.TableDescription;

/**
 * Created by ruedi on 14.08.2015.
 */
public class TableSpaceTest {

    @Test
    public void simple() {
        TableSpaceActor ts = Actors.AsActor(TableSpaceActor.class);
        ts.init(4, 0);
        runSimpleTest(ts);
        ts.shutDown().await();
    }

    @Test
    public void simpleRemote() {
        TableSpaceActor ts = Actors.AsActor(TableSpaceActor.class);
        ts.init(4, 0);

        new TCPNIOPublisher()

        runSimpleTest(ts);
        ts.shutDown().await();
    }

    protected void runSimpleTest(TableSpaceActor ts) {
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

        System.out.println(test.get("emil").await());
        test.filter( rec -> true, (r,e) -> System.out.println(r));

        Mutation mutation = test.getMutation();
        mutation.addOrUpdate("emil", "age", 9, "name", "Emil");
        mutation.addOrUpdate("felix", "age", 17, "name", "Felix");

        System.out.println(test.get("emil").await());
        test.filter( rec -> true, (r,e) -> System.out.println(r));
    }
}
