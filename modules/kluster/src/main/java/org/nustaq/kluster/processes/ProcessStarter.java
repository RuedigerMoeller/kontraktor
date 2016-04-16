package org.nustaq.kluster.processes;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by ruedi on 16/04/16.
 */
public class ProcessStarter extends Actor<ProcessStarter> {

    HashMap<String,ProcessStarter> siblings;
    String id;
    String hostName;
    Map<String,ProcessInfo> processes = new HashMap<>();
    int pids = 1;

    public void init( String hostName, String siblingHost, int siblingPort ) {
        siblings = new HashMap<>();
        id = UUID.randomUUID().toString();
        if ( hostName == null ) {
            try {
                hostName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                Log.Error(this,e);
                hostName = "unknown";
            }
        }
        if ( siblingHost != null ) {

        }
    }

    public IPromise terminateProcess( String id, boolean force, int timeoutSec ) {
        ProcessInfo processInfo = processes.get(id);
        if ( processInfo == null )
            return resolve(null);
        if ( force )
            processInfo.getProc().destroyForcibly();
        else
            processInfo.getProc().destroy();
        Promise res = new Promise();
        exec( () -> {
            processInfo.getProc().waitFor( timeoutSec, TimeUnit.SECONDS);
            if ( processInfo.getProc().isAlive() ) {
                res.reject("timeout");
            } else {
                processes.remove(processInfo.getId());
                res.resolve(processInfo);
            }
            return null;
        });
        return res;
    }

    public IPromise<ProcessInfo> startProcess( String workingDir, String ... commandLine ) {
        ProcessBuilder pc = new ProcessBuilder(commandLine);
        pc.directory(new File(workingDir));
        try {
            Process proc = pc.start();
            ProcessInfo pi = new ProcessInfo().cmdLine(commandLine).id(this.id + ":" + pids++).proc(proc);
            processes.put(pi.getId(), pi);
            return resolve(pi);
        } catch (IOException e) {
            Log.Warn(this, e);
            return reject(e);
        }
    }

    public IPromise<StarterDesc> getInstanceDesc() {
        return resolve(new StarterDesc().host(hostName).id(id).remoteRef(self()));
    }

    public static void main(String[] args) throws InterruptedException {
        ProcessStarter ps = Actors.AsActor(ProcessStarter.class);
        ps.init(null,null,0);
        new TCPNIOPublisher()
            .port(6767)
            .facade(ps)
            .publish( act -> {
                System.out.println("Discon "+act);
            });

        ProcessStarter remote = (ProcessStarter) new TCPConnectable(ProcessStarter.class,"localhost",6767).connect( (x,y) -> System.out.println("client disc "+x)).await();

        ProcessInfo bash = remote.startProcess("/tmp", "java","-version",">>","/tmp/xx.txt").await();

        System.out.println(bash);
        Thread.sleep(3000);
        Object await = remote.terminateProcess(bash.getId(), true, 15).await();
        System.out.println("term result "+await);

        //http://stackoverflow.com/questions/5740390/printing-my-macs-serial-number-in-java-using-unix-commands/5740673#5740673
        //http://stackoverflow.com/questions/1980671/executing-untokenized-command-line-from-java/1980921#1980921
    }

}
