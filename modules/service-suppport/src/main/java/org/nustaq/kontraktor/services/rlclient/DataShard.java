package org.nustaq.kontraktor.services.rlclient;

import org.nustaq.kontraktor.services.ServiceActor;
import org.nustaq.kontraktor.services.ServiceArgs;
import org.nustaq.kontraktor.services.ServiceDescription;
import org.nustaq.kontraktor.services.ServiceRegistry;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.impl.tablespace.TableSpaceActor;

import java.io.File;
import java.io.Serializable;

/**
 * Created by ruedi on 15.08.2015.
 */
public class DataShard extends ServiceActor<DataShard> {

    public static final String DATA_SHARD_NAME = "DataShard";
    TableSpaceActor tableSpace;

    @Override
    public IPromise init(ConnectableActor registryConnectable, ServiceArgs options, boolean auto /*ignored*/) {
        IPromise p = new Promise();
        try {
            super.init(registryConnectable, options,false).then( (r,e) -> {
                try {
                    initTableSpace();
                    registerSelf();
                    p.resolve();
                } catch (Exception ex) {
                    Log.Error(this,ex);
                }
            });
        } catch (Throwable t) {
            p.reject(t);
        }
        return p;
    }

    protected int getPort() {
        return cmdline.getDataShardPortBase()+getCmdline().getShardNo();
    }

    protected void initTableSpace() {
        DataCfg dataCfg = config.getDataCluster();
        tableSpace = Actors.AsActor( TableSpaceActor.class );
        String dir = dataCfg.getDataDir()[getCmdline().getShardNo()];
        new File(dir).mkdirs();
        tableSpace.setBaseDataDir(dir);
        tableSpace.init();
        tableSpace.ping().await();
        Log.Info(this, "init tablespace in "+
            dataCfg.getDataDir()[getCmdline().getShardNo()]+
            " shard "+ getCmdline().getShardNo()+" of "+dataCfg.getNumberOfShards()
        );
    }

    public IPromise<TableSpaceActor> getTableSpace() {
        return resolve(tableSpace);
    }

    @Override
    protected boolean needsDataCluster() {
        return false;
    }

    @Override
    protected String[] getRequiredServiceNames() {
        return new String[0];
    }

    @Override
    protected ServiceDescription createServiceDescription() {
        return new ServiceDescription(DATA_SHARD_NAME + getCmdline().getShardNo())
                .connectable( new TCPConnectable(DataShard.class, cmdline.getHost(), cmdline.getDataShardPortBase()+ getCmdline().getShardNo()));
    }

    protected DataShardArgs getCmdline() {
        return (DataShardArgs) cmdline;
    }

    @Override
    protected Serializable getStatus() {
        return "{ connections:"+(__connections != null ? __connections.size() : 0)+"}";
    }

    public static void main(String[] args) {
        start(args);
    }

    public static DataShard start(String[] args) {
        DataShardArgs options = (DataShardArgs) ServiceRegistry.parseCommandLine(args, null, DataShardArgs.New());
        return start(options);
    }

    public static DataShard start(DataShardArgs options) {
        DataShard ds = Actors.AsActor(DataShard.class,256000);
        ds.init(new TCPConnectable(ServiceRegistry.class, options.getRegistryHost(), options.getRegistryPort()), options, true); // .await(); fail ..
        Log.Info(ds.getClass(), "Init finished");
        return ds;
    }
}
