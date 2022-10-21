package cmd;

import com.beust.jcommander.Parameter;
import org.checkerframework.checker.units.qual.C;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.services.datacluster.DBClient;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.api.RealLiveTable;
import org.nustaq.reallive.api.TableDescription;
import org.nustaq.reallive.client.EmbeddedRealLive;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DBImporter extends DBClient {

    public static class DBImporterArgs extends DBClient.DBClientArgs {
        @Parameter(names={"-in"}, description ="directory to read files from", required = true)
        String sourceDir;

        @Parameter(names={"-minAgeHours"}, description ="only transfer if newer than now-given hours")
        int hours = 0;
        public String getSourceDir() {
            return sourceDir;
        }

        public int getHours() {
            return hours;
        }

        @Override
        public String toString() {
            return "DBImporterArgs{" +
                "sourceDir='" + sourceDir + '\'' +
                '}';
        }
    }
    protected DBClientArgs createArgs() {
        return new DBImporterArgs();
    }

    @Override
    protected void executeCode() {
        DBImporterArgs args = (DBImporterArgs) this.args;
        File f = new File(args.getSourceDir());
        File[] files = f.listFiles();
        long now = System.currentTimeMillis();
        long minAge = now-TimeUnit.HOURS.toMillis(args.getHours());
        Arrays.stream(files).forEach( file -> {
            if ( !file.isDirectory() && file.getName().endsWith(".bin") ) {
                String tableName = file.getName().substring(0,file.getName().lastIndexOf("_") );
                RealLiveTable remoteTable = dclient.tbl(tableName);
                if ( remoteTable != null ) {
                    CountDownLatch latch = new CountDownLatch(1);
                    TableDescription ts = new TableDescription()
                        .name(tableName)
                        .storageType(TableDescription.PERSIST)
                        .numEntries(100_000)
                        .filePath(file.getAbsolutePath())
                        .alternativePath(file.getAbsolutePath());
//                    System.out.println("open file "+file.getAbsolutePath());
                    RealLiveTable importTable = EmbeddedRealLive.get().loadTable(file.getAbsolutePath()).await();
                    AtomicInteger count = new AtomicInteger();
                    importTable.forEach( r -> true, (rec,err) -> {
                        if ( rec != null ) {
                            count.incrementAndGet();
                            if ( args.getHours() <= 0 || rec.getLastModified() > minAge )
                                remoteTable.setRecord(rec);
                        } else {
                            latch.countDown();
                            System.out.println("tablefile "+file.getName()+" entries "+count);
                        }
                    });
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        try {
            Thread.sleep(10_000); // ensure queues are empty
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        Log.setLevel(Log.ERROR);
        DBClient dbClient = new DBImporter();
        dbClient.connectAndStart(args, dbClient);
    }

}
