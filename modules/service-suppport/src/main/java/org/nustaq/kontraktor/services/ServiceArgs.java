package org.nustaq.kontraktor.services;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.nustaq.kontraktor.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by ruedi on 13/08/15.
 */
public class ServiceArgs {

    @Parameter(names={"-s","-servicereg"}, description = "serviceregistry host")
    String registryHost = "localhost";

    @Parameter(names={"-sp","-serviceregport"}, description = "serviceregistry port")
    int registry = 4567;

    @Parameter(names = {"-h","-help","-?", "--help"}, help = true, description = "display help")
    boolean help;

    @Parameter(names = {"-host"}, help = true, description = "host address/name of this service")
    private String host = "localhost";

    @Parameter(names = {"-dsPortBase"}, help = true, description = "port of data shard 0. port(shard_X) = portBase + X")
    private int dataShardPortBase = 30000;

    @Parameter(names = {"-nolog"}, help = true, description = "log to sysout without log4j", arity = 1)
    public boolean asyncLog = true;

    @Parameter(names = {"-filesystemConfigPreferred"}, description = "if true, prefer files on file system instead of files from classpath")
    private boolean filesystemConfigPreferred = true;

    @Parameter(names = {"replacingEnvVars"}, description = "if true, will replace environment variable placeholders in config files (e.g. ${SOME_ENV_VAR})")
    private boolean replacingEnvVars = false;

    public ServiceArgs() {
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            Log.Warn(this,e);
        }
    }

    public String getRegistryHost() {
        return registryHost;
    }

    public int getRegistryPort() {
        return registry;
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
                   "registryHost='" + registryHost + '\'' +
                   ", registry=" + registry +
                   ", help=" + help +
                   ", host='" + host + '\'' +
                   ", dataShardPortBase=" + dataShardPortBase +
                   '}';
    }

    public String getHost() {
        return host;
    }

    public boolean isAsyncLog() {
        return asyncLog;
    }

    public boolean isFilesystemConfigPreferred() {
        return filesystemConfigPreferred;
    }

    public boolean isReplacingEnvVars() {
        return replacingEnvVars;
    }

    public static ServiceArgs parseCommandLine(String[] args, ServiceArgs options) {

        JCommander com = new JCommander();
        com.addObject(options);
        try {
            com.parse(args);
        } catch (Exception ex) {
            System.out.println("command line error: '"+ex.getMessage()+"'");
            options.help = true;
        }
        if ( options.help ) {
            com.usage();
            System.exit(-1);
        }
        return options;
    }

}
