package org.nustaq.kluster.processes;

import com.beust.jcommander.JCommander;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ruedi on 17.04.16.
 */
public class StarterClient {

    public static void main(String[] args) throws IOException, InterruptedException {
        final StarterClientArgs options = new StarterClientArgs();
        new JCommander(options).parse(args);

        ProcessStarter starter = (ProcessStarter) new TCPConnectable(ProcessStarter.class, options.getHost(), options.getPort())
            .connect(
                (x, y) -> System.out.println("client disc " + x),
                act -> {
                    System.out.println("act " + act);
                }
            ).await();

        if ( options.isList()) {
            System.out.println("list:");
            List<ProcessInfo> pis = starter.getProcesses().await();
            pis.stream()
                .sorted( (a,b) -> a.getCmdLine()[0].compareTo(b.getCmdLine()[0]))
                .forEach( pi -> System.out.println(pi));
        }
        if ( options.getKillPid() != null ) {
            System.out.println("killing "+options.getKillPid());
            Object await = starter.terminateProcess(options.getKillPid(), true, 10).await();
            System.out.println("killed "+await);
        }
        String killMatching = options.getKillMatching();
        if ( killMatching != null ) {
            boolean hasCaps = false;
            for ( int i = 0; i < killMatching.length(); i++ ) {
                if ( Character.isUpperCase(killMatching.charAt(i)) ) {
                    hasCaps = true;
                    break;
                }
            }
            System.out.println("kill matching "+ killMatching);
            List<ProcessInfo> pis = starter.getProcesses().await();
            final boolean finalHasCaps = hasCaps;
            pis.stream()
                .forEach(pi -> {
                    String[] cmdLine = pi.getCmdLine();
                    boolean match = false;
                    for (int i = 0; i < cmdLine.length; i++) {
                        if (finalHasCaps) {
                            if (cmdLine[i].indexOf(killMatching) >= 0) {
                                match = true;
                                break;
                            }
                        } else {
                            if (cmdLine[i].toLowerCase().indexOf(killMatching) >= 0) {
                                match = true;
                                break;
                            }
                        }
                    }
                    if (match) {
                        System.out.print("killing " + pi + " .. ");
                        try {
                            Object await = starter.terminateProcess(pi.getId(), true, 10).await();
                            System.out.println(await + " ");
                        } catch (Exception e) {
                            System.out.println(""+e.getMessage());
                        }
                    }
                });
        }
        String[] cmd = new String[options.getParameters().size()];
        options.getParameters().toArray(cmd);
        if ( cmd.length > 0 ) {
            System.out.println("running "+ Arrays.toString(cmd));
            ProcessInfo await = starter.startProcess(new File(".").getCanonicalPath(), new HashMap<>(), cmd).await();
//        ProcessInfo await = starter.startProcess("/tmp", Collections.emptyMap(), "gnome-weather").await();
            System.out.println("started "+await);
        }
//        Thread.sleep(3000);
//        starter.terminateProcess(await.getId(),true,15).await();
//        System.out.println("killed "+await.getId());
        System.exit(0);
    }

}
