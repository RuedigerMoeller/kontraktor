package org.nustaq.kluster.processes;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ruedi on 17.04.16.
 */
public class StarterClientArgs {

    @Parameter(names = {"-host"}, help = true, description = "host address of this service")
    String host = "localhost";

    @Parameter
    private List<String> parameters = new ArrayList<>();

    @Parameter(names = {"-port"}, help = true, description = "port of this service")
    int port = 6868;

    public String getHost() {
        return host;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public int getPort() {
        return port;
    }

}
