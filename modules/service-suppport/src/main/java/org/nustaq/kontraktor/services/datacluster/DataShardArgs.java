package org.nustaq.kontraktor.services.datacluster;

import com.beust.jcommander.Parameter;
import org.nustaq.kontraktor.services.ServiceArgs;

import java.util.function.Supplier;

/**
 * Created by ruedi on 15.08.2015.
 */
public class DataShardArgs extends ServiceArgs {

    public static Supplier<DataShardArgs> factory = () -> new DataShardArgs();
    public static DataShardArgs New() {
        return factory.get();
    }

    public static DataShardArgs from(ServiceArgs toCopy, int shardNo) {
        DataShardArgs aNew = New();
        aNew.registryHost( toCopy.getRegistryHost() );
        aNew.registry( toCopy.getRegistryPort() );
        aNew.help(toCopy.isHelp());
        aNew.host(toCopy.getHost());
        aNew.dataShardPortBase(toCopy.getDataShardPortBase());
        aNew.asyncLog(toCopy.isAsyncLog());
        aNew.monhost(toCopy.getMonhost());
        aNew.monport(toCopy.getMonport());
        aNew.shardNo = shardNo;
        return aNew;
    }

    protected DataShardArgs() {
        super();
    }

    @Parameter( required = true, names = { "-sn","-shardNo" })
    int shardNo;

    @Parameter(names = {"-dsPortOverride"}, help = true, description = "override default exposure port (computed by portBase + shard_no)")
    private int dsPortOverride = 0;

    public int getShardNo() {
        return shardNo;
    }

    public int getDsPortOverride() {
        return dsPortOverride;
    }

    @Override
    public String toString() {
        return "DataShardArgs{" +
                   "shardNo=" + shardNo +
                   "} " + super.toString();
    }


    public DataShardArgs factory(Supplier<DataShardArgs> factory) {
        this.factory = factory;
        return this;
    }

    public DataShardArgs shardNo(int shardNo) {
        this.shardNo = shardNo;
        return this;
    }

    public DataShardArgs dsPortOverride(int dsPortOverride) {
        this.dsPortOverride = dsPortOverride;
        return this;
    }
}
