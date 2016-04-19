package org.nustaq.kluster.processes;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by ruedi on 19/04/16.
 */
public class ProcStartSpec implements Serializable {

    String redirectIO;
    String aSiblingId;
    String aSiblingName;
    String workingDir;
    Map<String,String> env;
    String [] commandLine;

    public ProcStartSpec( String redirectIO, String aSiblingId, String aSiblingName, String workingDir, Map<String,String> env, String ... commandLine ) {
        this.redirectIO = redirectIO;
        this.aSiblingId = aSiblingId;
        this.aSiblingName = aSiblingName;
        this.workingDir = workingDir;
        this.env = env;
        this.commandLine = commandLine;
    }

    public String getRedirectIO() {
        return redirectIO;
    }

    public String getaSiblingId() {
        return aSiblingId;
    }

    public String getaSiblingName() {
        return aSiblingName;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public String[] getCommandLine() {
        return commandLine;
    }

}
