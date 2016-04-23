package org.nustaq.kluster.processes;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by ruedi on 19/04/16.
 */
public class ProcStartSpec implements Serializable {

    String shortName;
    String group;
    String redirectIO;
    String aSiblingId;
    String aSiblingName;
    String workingDir;
    Map<String,String> env;
    String [] commandLine;

    public ProcStartSpec( String group, String shortName, String redirectIO, String aSiblingId, String aSiblingName, String workingDir, Map<String,String> env, String ... commandLine ) {
        this.redirectIO = redirectIO;
        this.aSiblingId = aSiblingId;
        this.aSiblingName = aSiblingName;
        this.workingDir = workingDir;
        this.env = env;
        this.commandLine = commandLine;
        this.shortName = shortName;
        this.group = group;
        if ( this.group == null ) {
            this.group = "NONE";
        }
        if ( this.shortName == null ) {
            this.shortName = deriveShortname(commandLine);
        }
    }

    public static String deriveShortname(String[] commandLine) {
        return Arrays.stream(commandLine).filter( s -> s != null && ! s.startsWith("-") ).collect(Collectors.joining(" "));
    }

    public String getShortName() {
        return shortName;
    }

    public String getSortString() {
        return getGroup()+"#"+getShortName();
    }
    public String getGroup() {
        return group;
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
