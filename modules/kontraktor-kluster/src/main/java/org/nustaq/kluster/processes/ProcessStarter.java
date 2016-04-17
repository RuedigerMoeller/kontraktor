package org.nustaq.kluster.processes;

import com.beust.jcommander.JCommander;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by ruedi on 16/04/16.
 */
public class ProcessStarter extends Actor<ProcessStarter> {

    HashMap<String,StarterDesc> siblings;
    ConnectableActor primarySibling;
    StarterDesc primaryDesc;
    String id;
    String name;
    Map<String,ProcessInfo> processes = new HashMap<>();
    int pids = 1;
    StarterArgs options;

    public void init( StarterArgs options ) {
        this.options = options;
        siblings = new HashMap<>();
        id = UUID.randomUUID().toString();
        if ( options.getName() == null ) {
            try {
                this.name = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                Log.Error(this,e);
                this.name = "unknown";
            }
        } else {
            name = options.getName();
        }
        final String siblingHost = options.getSiblingHost();
        if ( siblingHost != null ) {
            final int siblingPort = options.getSiblingPort();
            primarySibling = new TCPConnectable(ProcessStarter.class, siblingHost, siblingPort);
            cycle();
        }
    }

    void initPrimary() {
        try {
            ProcessStarter sibling =
                (ProcessStarter) primarySibling
                    .connect(
                        (x, y) -> System.out.println("client disc " + x),
                        act -> {
                            System.out.println("act " + act);
                            siblings.entrySet().forEach(en -> self().execute(() -> siblings.remove(en.getKey())));
                        }
                    ).await();
            primaryDesc = sibling.getInstanceDesc().await();
            siblings.put(primaryDesc.getId(),primaryDesc);
            System.out.println("primary sibling connected "+primaryDesc);
        } catch (Throwable e) {
            System.out.println("failed to connect primary "+primarySibling);
        }
    }

    public void cycle() {
        if ( ! isStopped() ) {
            if ( primaryDesc == null ) {
                initPrimary();
            }
            if ( primarySibling != null ) {
                discoverSiblings(primaryDesc);
            }
            delayed(3_000, () -> cycle() );
        }
    }

    public IPromise<Map<String,StarterDesc>> register(StarterDesc other) {
        if ( ! siblings.containsKey(other.getId()) )
            siblings.put(other.getId(), other);
        return resolve(siblings);
    }

    private void discoverSiblings(StarterDesc desc) {
        if ( desc == null || desc.getId() == null || desc.getId().equals(id) )
            return;
        if (desc.getRemoteRef().isStopped()) {
            execute( () -> siblings.remove(desc.getId()) );
        } else {
            desc.getRemoteRef().register(getDesc()).then((rsib, err) -> {
                rsib.forEach((rid, rdesc) -> {
                    if (!siblings.containsKey(id) && !this.id.equals(id)) {
                        siblings.put(rid,rdesc);
                        discoverSiblings(rdesc);
                    }
                });
            });
        }
    }

    public IPromise<Integer> terminateProcess( String id, boolean force, int timeoutSec ) {
        ProcessInfo processInfo = processes.get(id);
        if ( processInfo == null )
            return resolve(null);

        long pid = -1;
        try {
            Field f = processInfo.getProc().getClass().getDeclaredField("pid");
            f.setAccessible(true);
            pid = f.getLong(processInfo.getProc());
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        if ( pid != -1 ) {
            try {
                Process kl = Runtime.getRuntime().exec("kill -9 " + pid);
                kl.waitFor(timeoutSec, TimeUnit.SECONDS);
                return resolve(new Integer(kl.exitValue()));
            } catch (IOException e) {
                e.printStackTrace();
                return reject(e);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            if ( force )
                processInfo.getProc().destroyForcibly();
            else
                processInfo.getProc().destroy();
        }
        Promise res = new Promise();
        exec( () -> {
            processInfo.getProc().waitFor( timeoutSec, TimeUnit.SECONDS);
            if ( processInfo.getProc().isAlive() ) {
                res.reject("timeout");
            } else {
                processes.remove(processInfo.getId());
                res.resolve(processInfo.getProc().exitValue());
            }
            return null;
        });
        return res;
    }

    public IPromise<ProcessInfo> startProcess( String anId, String aName, String workingDir, Map<String,String> env, String ... commandLine ) {
        if ( this.name.equals(aName) )
            aName = null;
        if ( this.id.equals(anId) )
            anId = null;
        if ( aName != null && ! aName.equals(this.name)) {
            if ( anId != null )
                return reject("cannot specify name and id: "+aName+" "+anId);
            final String finalName = aName;
            Optional<StarterDesc> first = siblings.values().stream().filter(desc -> finalName.equals(desc.getName())).findFirst();
            if ( first.isPresent() ) {
                anId = first.get().getId();
            }
        }
        if ( anId != null && ! anId.equals(this.id) ) {
            StarterDesc starterDesc = siblings.get(anId);
            if ( starterDesc != null ) {
                return starterDesc.getRemoteRef().startProcess(anId,aName,workingDir,env,commandLine);
            } else {
                return reject( "no sibling known with id "+anId);
            }
        }
        if ( (anId != null || aName != null) ) {
            return reject("could not find target for "+anId+" "+aName);
        }
        ProcessBuilder pc = new ProcessBuilder(commandLine);
        if ( env != null ) {
            pc.environment().putAll(env);
        }

        pc.directory(new File(workingDir));
        try {
            Process proc = pc.start();
            ProcessInfo pi = new ProcessInfo()
                .cmdLine(commandLine)
                .id(this.id + ":" + pids++)
                .proc(proc)
                .starterName(this.name)
                .starterId(this.id);
            processes.put(pi.getId(), pi);
            return resolve(pi);
        } catch (IOException e) {
            Log.Warn(this, e);
            return reject(e);
        }
    }

    public IPromise<StarterDesc> getInstanceDesc() {
        return resolve(getDesc());
    }

    private StarterDesc getDesc() {
        return new StarterDesc().host(options.getHost()).name(name).id(id).remoteRef(self()).port(options.getPort());
    }

    public IPromise<List<StarterDesc>> getSiblings() {
        List<StarterDesc> collect = siblings.values().stream().collect(Collectors.toList());
        collect.add(getDesc());
        return resolve(collect);
    }

    public IPromise<List<ProcessInfo>> getProcesses() {
        return resolve(processes.entrySet().stream().map( x -> x.getValue() ).collect(Collectors.toList()));
    }

    public static void main(String[] args) throws InterruptedException {

        final StarterArgs options = new StarterArgs();
        new JCommander(options).parse(args);

        ProcessStarter ps = Actors.AsActor(ProcessStarter.class);
        ps.init(options);

        new TCPNIOPublisher()
            .port(options.getPort())
            .facade(ps)
            .publish( act -> {
                System.out.println("Discon "+act);
            });

        // testing
//        ProcessStarter remote = (ProcessStarter) new TCPConnectable(ProcessStarter.class,options.getHost(),options.getPort()).connect( (x,y) -> System.out.println("client disc "+x)).await();
//        ProcessInfo bash = remote.startProcess("/tmp", Collections.emptyMap(), "bash", "-c", "xclock -digital").await();
//
//        List<ProcessInfo> procs = remote.getProcesses().await();
//        procs.forEach( proc -> System.out.println(proc));
//

//        Thread.sleep(3000);
//
//
//        Object await = remote.terminateProcess(bash.getId(), true, 15).await();
//        System.out.println("term result "+await);

        //http://stackoverflow.com/questions/5740390/printing-my-macs-serial-number-in-java-using-unix-commands/5740673#5740673
        //http://stackoverflow.com/questions/1980671/executing-untokenized-command-line-from-java/1980921#1980921
    }

}
