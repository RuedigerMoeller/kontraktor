package org.nustaq.kontraktor.services;

import com.beust.jcommander.Parameter;
import org.nustaq.kontraktor.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by ruedi on 13/08/15.
 */
public class ServiceArgs {

    @Parameter(names={"-g","-gravity"}, description = "gravity host")
    String gravityHost = "localhost";

    @Parameter(names={"-gp","-gravity port"}, description = "gravity port")
    int gravityPort = 4567;

    @Parameter(names = {"-h","-help","-?", "--help"}, help = true, description = "display help")
    boolean help;

    @Parameter(names = {"-host"}, help = true, description = "host address/name of this service")
    private String host = "localhost";

    @Parameter(names = {"-dsPortBase"}, help = true, description = "port of data shard 0. port(shard_X) = portBase + X")
    private int dataShardPortBase = 30000;

    @Parameter(names = {"-nolog"}, help = true, description = "log to sysout without log4j", arity = 1)
    public boolean sysoutlog = true;

    public ServiceArgs() {
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            Log.Warn(this,e);
        }
    }

    public String getGravityHost() {
        return gravityHost;
    }

    public int getGravityPort() {
        return gravityPort;
    }

    public int getDataShardPortBase() {
        return dataShardPortBase;
    }

    public boolean isHelp() {
        return help;
    }

    @Override
    public String toString() {
        return "ServiceArgs{" +
                   "gravityHost='" + gravityHost + '\'' +
                   ", gravityPort=" + gravityPort +
                   ", help=" + help +
                   ", host='" + host + '\'' +
                   ", dataShardPortBase=" + dataShardPortBase +
                   '}';
    }

    public String getHost() {
        return host;
    }

    public boolean isSysoutlog() {
        return sysoutlog;
    }


}
