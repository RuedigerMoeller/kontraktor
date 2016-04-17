package org.nustaq.kluster.processes;

import com.beust.jcommander.JCommander;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

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

        String[] cmd = new String[options.getParameters().size()];
        options.getParameters().toArray(cmd);
        System.out.println("running "+ Arrays.toString(cmd));
//        ProcessInfo await = starter.startProcess(new File(".").getCanonicalPath(), new HashMap<>(), cmd).await();
        ProcessInfo await = starter.startProcess("/tmp", Collections.emptyMap(), "bash", "-c", "xclock -digital").await();
        System.out.println("started "+await);
        Thread.sleep(3000);
        starter.terminateProcess(await.getId(),true,15).await();
    }

}
