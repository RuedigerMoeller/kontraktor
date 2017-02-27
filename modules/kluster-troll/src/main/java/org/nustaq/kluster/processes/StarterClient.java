package org.nustaq.kluster.processes;

import com.beust.jcommander.JCommander;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by ruedi on 17.04.16.
 */
public class StarterClient {

    public void run(String[] args) throws Exception {

        if ( args.length >= 1 && args[args.length-1].endsWith(".kson") ) {

            final StarterClientArgs options = new StarterClientArgs();
            final JCommander jCommander = new JCommander(options);
            parseStarterConf(args, options, jCommander);
            options.underride(ProcessStarter.locateProps(0,new File("./"),"troll.properties"));

            // special to process a whole cluster definition from a file (quite a quick hack)
            HashSet<String> groups = new HashSet<>();
            for (int i = 0; i < options.getParameters().size()-1; i++) {
                String arg = options.getParameters().get(i);
                groups.add(arg);
            }
            KlusterConf klusterConf = new KlusterConf( groups, args[args.length-1]);
            if ( klusterConf.getToStart().size() == 0 ) {
                System.out.println("no processes found in "+args[0]);
                System.exit(0);
            }
            klusterConf.getToStart().forEach( proc -> {
                System.out.println("will try "+proc.getGroup()+" "+proc.getProcessShortName());
            });
            try {
                ProcessStarter starter = (ProcessStarter) new TCPConnectable(ProcessStarter.class, options.getHost(), options.getPort())
                    .connect(
                        (x, y) -> System.out.println("client disc " + x),
                        act -> {
                            System.out.println("act " + act);
                        }
                    ).await();
                klusterConf.getToStart().forEach( opts -> {
                    try {
                        runProc(opts, starter);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                starter.ping().await();
            } catch (Throwable th) {
                System.out.println("could not connect "+th+" host:"+options.getHost()+" "+options.getPort());
                System.exit(-1);
            }
        } else {
            final StarterClientArgs options = new StarterClientArgs();
            final JCommander jCommander = new JCommander(options);

            parseStarterConf(args, options, jCommander);
            options.underride( ProcessStarter.locateProps() );

            if ( options.isHelp() ) {
                jCommander.usage();
                System.exit(0);
            }

            try {
                ProcessStarter starter = (ProcessStarter) new TCPConnectable(ProcessStarter.class, options.getHost(), options.getPort())
                    .connect(
                        (x, y) -> System.out.println("client disc " + x),
                        act -> {
                            System.out.println("act " + act);
                        }
                    ).await();

                runProc(options, starter);
                starter.ping().await();
            } catch (Throwable th) {
                System.out.println("could not connect "+th+" host:"+options.getHost()+" "+options.getPort());
                System.exit(-1);
            }
        }

        System.exit(0);
    }

    public void runProc(StarterClientArgs options, ProcessStarter starter) throws IOException {

        if (options.isResync())
            starter.resyncProcesses();

        if (options.getRestartIdOrName()!=null)
            restartIdOrName(starter,options.getRestartIdOrName());

        if ( options.isListDetailed()) {
            List<ProcessInfo> pis = starter.getProcesses().await();
            System.out.println("listing "+pis.size()+" processes:");
            pis.stream()
                .sorted( (a,b) -> a.getCmdLine()[0].compareTo(b.getCmdLine()[0]))
                .forEach( pi -> System.out.println(pi));
        }
        if ( options.isList()) {
            List<ProcessInfo> pis = starter.getProcesses().await();
            System.out.println("listing "+pis.size()+" processes:");
            pis.stream()
                .sorted((a, b) -> a.getSpec().getSortString().compareTo(b.getSpec().getSortString()))
                .forEach(pi -> System.out.println(pi.getSpec().getGroup() + "\t " + pi.getStarterName() + "\t "+pi.getSpec().getShortName() ) );
        }
        if ( options.isListSiblings() ) {
            List<StarterDesc> await = starter.getSiblings().await();
            System.out.println("listing "+await.size()+" siblings");
            await.forEach( sd -> System.out.println(sd) );
        }
        if ( options.getKillPid() != null ) {
            System.out.println("killing group, name or id:"+options.getKillPid());
            killMatching(starter, options.getKillPid(), false);
//            Object await = starter.terminateProcess(options.getKillPid(), true, 10).await();
//            System.out.println("killed "+await);
        }
        String killMatching = options.getKillMatching();
        if ( killMatching != null ) {
            System.out.println("kill matching "+ killMatching);
            killMatching(starter, killMatching, true);
        }
        String[] cmd = new String[options.getParameters().size()];
        options.getParameters().toArray(cmd);
        if ( cmd.length > 0 ) {
            System.out.println("try '" + Arrays.stream(cmd).collect(Collectors.joining(" "))+"'");
            ProcessInfo await = starter.startProcess( options.getGroup(), options.getProcessShortName(), options.getRedirect(),options.getId(), options.getName(), options.getWd(), new HashMap<>(), cmd).await();
            System.out.println("started " + await);
        }
        if ( options.getSleep() > 0 ) {
            try {
                Thread.sleep(options.getSleep());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void killMatching(ProcessStarter starter, String killMatching, boolean substrSearch) {
        Stream<ProcessInfo> matchinPis = getMatchingProcessInfosFromRunningProcesses(starter, killMatching,substrSearch);
        matchinPis
            .forEach(pi -> {
                    System.out.print("killing " + pi + " .. ");
                    try {
                        Object await = starter.terminateProcess(pi.getId(), true, 10).await();
                        System.out.println(await + " ");
                    } catch (Exception e) {
                        System.out.println("" + e.getMessage());
                    }
                }
            );
    }

    public void restartIdOrName(ProcessStarter starter, String restartIdOrName) {
            System.out.println("restart matching "+ restartIdOrName);
            Stream<ProcessInfo> matchinPis = getMatchingProcessInfosFromRunningProcesses(starter, restartIdOrName, false);
            matchinPis
                .forEach(pi -> {
                        System.out.println("killing " + pi + " .. ");
                        try {
                            Integer await = starter.terminateProcess(pi.getId(), true, 10).await();
                            System.out.println("terminated with code "+ await );
                            ProcStartSpec debug = pi.getSpec();
                            ProcessInfo newPI = starter.startProcessBySpec(debug).await();
                            System.out.println("started "+ newPI );
                        } catch (Exception e) {
                            System.out.println("" + e.getMessage());
                        }
                    }
                );
    }

    public static boolean hasCaps(String killMatching) {
        boolean hasCaps = false;
        for ( int i = 0; i < killMatching.length(); i++ ) {
            if ( Character.isUpperCase(killMatching.charAt(i)) ) {
                hasCaps = true;
                break;
            }
        }
        return hasCaps;
    }

    public Stream<ProcessInfo> getMatchingProcessInfosFromRunningProcesses(ProcessStarter starter, String pidOrGroupOrNameOrSub, boolean matchBySubstring) {
        List<ProcessInfo> pis = starter.getProcesses().await();
        boolean finalHasCaps = hasCaps(pidOrGroupOrNameOrSub);
        return pis.stream()
            .filter(pi -> {
                String[] cmdLine = pi.getCmdLine();
                if (pi.getSpec().getShortName().equals(pidOrGroupOrNameOrSub))
                    return true;
                if (pi.getSpec().getGroup().equals(pidOrGroupOrNameOrSub))
                    return true;
                if (pi.getId().equals(pidOrGroupOrNameOrSub))
                    return true;
                if (!matchBySubstring)
                    return false;
                boolean match = false;
                for (int i = 0; i < cmdLine.length; i++) {
                    if (finalHasCaps) {
                        if (cmdLine[i].indexOf(pidOrGroupOrNameOrSub) >= 0) {
                            match = true;
                            break;
                        }
                    } else {
                        if (cmdLine[i].toLowerCase().indexOf(pidOrGroupOrNameOrSub) >= 0) {
                            match = true;
                            break;
                        }
                    }
                }
                return (match || "all".equals(pidOrGroupOrNameOrSub));
            });
    }

    public static StarterClientArgs parseStarterConf(String[] args, StarterClientArgs inOutOptions, JCommander jCommander) {
        String lastParms = null;
        // avoid jcommander dequoting + parse failure
        if ( args.length > 0 && args[args.length-1].indexOf(" ") > 0 ) // multiword
        {
            String newargs[] = new String[args.length-1];
            for (int i = 0; i < newargs.length; i++) {
                newargs[i] = args[i];
            }
            lastParms = args[args.length-1];
            args = newargs;
        }
        try {
            jCommander.parse(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("PARSE FAILURE "+Arrays.toString(args));
        }
        try {
            inOutOptions.underride(ProcessStarter.locateProps());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if ( lastParms != null ) {
            String[] split = lastParms.split(" ");
            Arrays.stream(split).forEach( x -> inOutOptions.getParameters().add(x));
        }
        return inOutOptions;
    }

    public static void main(String[] args) throws Exception {
        new StarterClient().run(args);
    }

}
