package org.nustaq.kontraktor.templateapp;

import com.beust.jcommander.Parameter;
import org.nustaq.kontraktor.services.ServiceArgs;

/**
 * Created by ruedi on 29.05.17.
 */
public class WebServerArgs extends ServiceArgs {

    @Parameter( names = "-webhost")
    String webHost = "localhost";
    @Parameter( names = "-webport")
    int webPort = 8888;
    @Parameter( names = "-prod")
    Boolean prod = false;

}
