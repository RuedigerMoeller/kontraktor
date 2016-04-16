package org.nustaq.kluster.processes;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by ruedi on 16/04/16.
 */
public class ProcessInfo implements Serializable {

    String id;
    ProcessStarter starter;
    String[] cmdLine;
    transient Process proc;

    public Process getProc() {
        return proc;
    }

    public ProcessInfo proc(final Process proc) {
        this.proc = proc;
        return this;
    }

    public String getId() {
        return id;
    }

    public String[] getCmdLine() {
        return cmdLine;
    }

    public ProcessInfo cmdLine(final String cmdLine[]) {
        this.cmdLine = cmdLine;
        return this;
    }

    public ProcessStarter getStarter() {
        return starter;
    }

    public ProcessInfo id(final String id) {
        this.id = id;
        return this;
    }

    public ProcessInfo starter(final ProcessStarter starter) {
        this.starter = starter;
        return this;
    }

    @Override
    public String toString() {
        return "ProcessInfo{" +
            "id='" + id + '\'' +
            ", starter=" + starter +
            ", cmdLine='" + Arrays.toString(cmdLine) + '\'' +
            ", proc=" + proc +
            '}';
    }
}
