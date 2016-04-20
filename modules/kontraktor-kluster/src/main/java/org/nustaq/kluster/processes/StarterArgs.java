package org.nustaq.kluster.processes;

import com.beust.jcommander.Parameter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

/**
 * Created by ruedi on 16.04.16.
 */
public class StarterArgs {

    @Parameter(names={"-shost"}, description = "sibling host")
    String siblingHost = null;

    @Parameter(names={"-sport"}, description = "sibling port")
    int siblingPort = 0;

    @Parameter(names = {"-h","-help","-?", "--help"}, help = true, description = "display help")
    boolean help;

    @Parameter(names = {"-host"}, help = true, description = "host address of this service")
    String host = null;

    @Parameter(names = {"-name"}, help = true, description = "name of this service")
    String name = null;

    @Parameter(names = {"-port"}, help = true, description = "port of this service")
    int port = 0;

    public String getSiblingHost() {
        return siblingHost;
    }

    public String getName() {
        return name;
    }

    public int getSiblingPort() {
        return siblingPort;
    }

    public boolean isHelp() {
        return help;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    /**
     * fill gaps by property lookup
     * @param props
     */
    public void underride( Properties props ) {
        if ( siblingHost == null ) {
            siblingHost = props.getProperty("shost");
        }
        if ( siblingPort == 0 ) {
            siblingPort = Integer.parseInt(props.getProperty("sport"));
        }
        if ( host == null ) {
            host = props.getProperty("host");
            if ( host == null ) {
                try {
                    host = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }
        if ( port == 0 ) {
            port = Integer.parseInt(props.getProperty("port"));
        }
        if ( name == null ) {
            name = props.getProperty("name");
        }
    }
}
