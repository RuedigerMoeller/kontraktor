package org.nustaq.kontraktor.services.datacluster;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.remoting.base.ServiceDescription;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.services.ClusterCfg;
import org.nustaq.kontraktor.services.ServiceActor;
import org.nustaq.kontraktor.services.datacluster.dynamic.DynDataServiceRegistry;
import org.nustaq.kontraktor.services.datacluster.dynamic.DynDataShard;
import org.nustaq.kontraktor.services.rlclient.DataClient;
import org.nustaq.reallive.api.RealLiveTable;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.server.dynamic.DynClusterDistribution;

import java.util.HashMap;
import java.util.Map;

/**
 * enables standalone connectivity to a data cluster (no service required). Only supports dynamic data clusters
 */
public class DBClient {
    protected DBClientArgs args;
    protected DynDataServiceRegistry registry;
    protected DataClient dclient;

    public DBClient() {
    }

    public DataClient connect(DBClientArgs dbClientArgs) {
        this.args = dbClientArgs;
        TCPConnectable tcpConnectable = new TCPConnectable(DynDataServiceRegistry.class, args.getRegistryHost(), args.getRegistryPort());
        registry = (DynDataServiceRegistry) tcpConnectable.connect( (ac, err) -> {
            System.out.println("disconnected "+ac+" "+err);
        }, actor -> {
            System.out.println("disconnected "+actor);
        }).await();

        Map<String, ServiceDescription> sm = registry.getServiceMap().await();
        sm.forEach( (k,v) -> System.out.println(k+"\t"+v));
        Map<String,DynDataShard> shardMap = new HashMap<>();
        sm.forEach( (k,v) -> {
            if ( isDataShard(k) ) {
                shardMap.put(k, (DynDataShard) v.getConnectable().connect().await());
            }
        });
        ClusterCfg cluster = registry.getConfig().await();
        DynClusterDistribution activeDistribution = registry.getActiveDistribution().await();
        dclient = ServiceActor.InitRealLiveDynamic(
            activeDistribution,
            registry,
            name -> shardMap.get(name),
            null,
            cluster.getDataCluster()
        );

        return dclient;
    }

    public DataClient getDClient() {
        return dclient;
    }

    public DBClientArgs getArgs() {
        return args;
    }

    public DynDataServiceRegistry getRegistry() {
        return registry;
    }

    private boolean isDataShard(String name) {
        return name.startsWith("DynShard") || name.startsWith("DataShard");
    }

    public static class DBClientArgs {
        @Parameter(names={"-s","-servicereg"}, description = "serviceregistry host")
        String registryHost = "localhost";

        @Parameter(names={"-sp","-serviceregport"}, description = "serviceregistry port")
        int registryPort = 4567;

        @Parameter(names = {"-h","-help","-?", "--help"}, help = true, description = "display help")
        boolean help;

        public String getRegistryHost() {
            return registryHost;
        }

        public int getRegistryPort() {
            return registryPort;
        }

        public boolean isHelp() {
            return help;
        }

        @Override
        public String toString() {
            return "DBClientArgs{" +
                "registryHost='" + registryHost + '\'' +
                ", registry=" + registryPort +
                ", help=" + help +
                '}';
        }
    }


    public void connectAndStart(String[] args, DBClient dbClient) {
        DBClientArgs dbClientArgs = createArgs();
        JCommander jc = new JCommander(dbClientArgs);
        try {
            jc.parse(args);
            dbClient.connect(dbClientArgs);
            dbClient.executeCode();
        } catch (Exception e) {
            e.printStackTrace();
            jc.usage();
        }
        if (dbClientArgs.isHelp())
        {
            jc.usage();
            System.exit(0);
        }
    }

    protected DBClientArgs createArgs() {
        return new DBClientArgs();
    }

    protected void exit( int code ) {
        SimpleScheduler.DelayedCall(1000, () -> System.exit(code));
    }

    protected void executeCode() {
//        RealLiveTable feed = dclient.tbl("feed");
//        RealLiveTable feedEntity = dclient.tbl("feedEntity");
//        Record jonny = feedEntity.get("42").await();
//        System.out.println(jonny.toPrettyString());
//        System.out.println("---------------------------------------------------");
//        feed.query("type ** 'status'", (r, e) -> {
//            if ( r != null )
//                System.out.println(r.toPrettyString());
//            else
//                System.out.println("query finished");
//        });
    }

    public static void main(String[] args) {
        DBClient dbClient = new DBClient();
        dbClient.connectAndStart(args, dbClient);
    }
}
