package org.nustaq.kontraktor.services.rlclient;

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

    protected DataShardArgs() {
        super();
    }

    @Parameter( required = true, names = { "-sn","-shardNo" })
    int shardNo;

    public int getShardNo() {
        return shardNo;
    }

    @Override
    public String toString() {
        return "DataShardArgs{" +
                   "shardNo=" + shardNo +
                   "} " + super.toString();
    }
}
