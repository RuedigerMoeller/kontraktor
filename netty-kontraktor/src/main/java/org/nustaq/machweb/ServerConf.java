package org.nustaq.machweb;

import java.util.HashMap;
import java.util.List;

/**
 * Created by ruedi on 25.10.2014.
 */
public class ServerConf {

    public int port = 7777;
    public int clientThreads = 2;
    public int clientQSize = 1000;
    public String componentPath[];
    public String components[];
    public String scan4Remote[];

    public List userData;

}
