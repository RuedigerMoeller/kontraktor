package org.nustaq.kluster.processes;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by ruedi on 17.04.16.
 */
public class StarterClientArgs implements Serializable {

    public StarterClientArgs() {
    }

    @Parameter(names = {"-host"}, help = true, description = "host address of process starter")
    String host;

    @Parameter
    private List<String> parameters = new ArrayList<>();

    @Parameter(names = {"-port"}, help = true, description = "port of of process starter")
    int port;

    @Parameter(names = {"-l"}, help = true, description = "list")
    boolean list = false;

    @Parameter(names = {"-ls"}, help = true, description = "list siblings")
    boolean listSiblings = false;

    @Parameter(names = {"-k"}, arity = 1, help = true, description = "kill [processid]")
    String pid = null;

    @Parameter(names = {"-km"}, arity = 1, help = true, description = "kill [pattern]")
    String killMatching = null;

    @Parameter(names = {"-wd"}, help = true, description = "workingdir")
    String wd = null;

    @Parameter(names = {"-id"}, help = true, description = "id of target sibling")
    String id;

    @Parameter(names = {"-name"}, help = true, description = "name of target sibling")
    String name;

    @Parameter(names = {"-redirect"}, help = true, description = "name remote file to redirect output to")
    String redirect;

    @Parameter(names = {"-resync"}, help = true, description = "resync processes and siblings")
    boolean resync;

    @Parameter(names = {"-restart"}, help = true, description = "restart process with given id or name")
    String restartIdOrName;

    @Parameter(names = {"-sleep"}, help = true, description = "sleep (milli seconds) after starting a process")
    long sleep;

    @Parameter(names = {"-h","-help","-?", "--help"}, help = true, description = "display help")
    boolean help;

    public long getSleep() {
        return sleep;
    }

    public String getRestartIdOrName() {
        return restartIdOrName;
    }

    public boolean isHelp() {
        return help;
    }

    public boolean isResync() {
        return resync;
    }

    public boolean isList() {
        return list;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getKillMatching() {
        return killMatching;
    }

    public String getKillPid() {
        return pid;
    }

    public String getWd() {
        return wd;
    }

    public String getHost() {
        return host;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public int getPort() {
        return port;
    }

    public boolean isListSiblings() {
        return listSiblings;
    }

    public String getRedirect() {
        return redirect;
    }

    /**
     * fill gaps by property lookup
     * @param props
     */
    public void underride( Properties props ) {
        if ( host == null ) {
            host = props.getProperty("host");
        }
        if ( port == 0 ) {
            port = Integer.parseInt(props.getProperty("port"));
        }
        if ( name == null ) {
            name = props.getProperty("name");
        }
        if ( wd == null ) {
            wd = props.getProperty("wd");
        }
    }

    @Override
    public String toString() {
        return "StarterClientArgs{" +
            "host='" + host + '\'' +
            ", parameters=" + parameters +
            ", port=" + port +
            ", list=" + list +
            ", listSiblings=" + listSiblings +
            ", pid='" + pid + '\'' +
            ", killMatching='" + killMatching + '\'' +
            ", wd='" + wd + '\'' +
            ", id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", redirect='" + redirect + '\'' +
            ", resync=" + resync +
            ", help=" + help +
            '}';
    }
}
