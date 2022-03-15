package org.nustaq.kontraktor.services.rlserver;

import org.nustaq.kontraktor.services.ClusterCfg;
import org.nustaq.kontraktor.services.datacluster.DataCfg;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kson.Kson;
import org.nustaq.reallive.api.TableDescription;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

public class SimpleRLConfig implements Serializable {

    public static SimpleRLConfig get;

    protected String dataDir = "./data";
    protected int numNodes = 3;
    protected TableDescription tables[];
    protected int numSessionThreads = 4;
    protected int bindPort = 8081;
    protected boolean runDataClusterInsideWebserver = true;

    // API endpoint
    protected String publicUrl = "http://localhost:"+bindPort;
    protected String bindIp = "0.0.0.0";
    protected String wsPublicUrl = "ws://localhost:"+bindPort+"/ws";
    protected String mongoConnection; //"mongodb+srv://<username>:<password>@<cluster-address>/test?w=majority"
    protected int tcpPort = 7654;

    protected Map<String,Object> customData;

    protected int sessionTimeoutMinutes = 10;

    public static String pathname = "./etc/reallive.kson";

    public static SimpleRLConfig read() {
        return read(pathname);
    }

    public static SimpleRLConfig read(String pathname) {
        return read(pathname,SimpleRLConfig.class);
    }

    public static SimpleRLConfig read(String pathname,Class<? extends SimpleRLConfig> simpleRLConfigClass) {
        Kson kson = new Kson().map(simpleRLConfigClass, TableDescription.class );
        try {
            SimpleRLConfig clCfg = (SimpleRLConfig) kson.readObject(new File(pathname));
            String confString = kson.writeObject(clCfg);
            System.out.println("run with config from "+ new File(pathname).getCanonicalPath());
//            System.out.println(confString);
            get = clCfg;
            return clCfg;
        } catch (Exception e) {
            e.printStackTrace();
            Log.Warn(null, pathname + " not found or parse error. " + e.getClass().getSimpleName() + ":" + e.getMessage());
            try {
                String sampleconf = kson.writeObject(new SimpleRLConfig());
                System.out.println("Try:\n"+sampleconf);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            System.exit(1);
        }
        return new SimpleRLConfig();
    }

    public boolean isRunDataClusterInsideWebserver() {
        return runDataClusterInsideWebserver;
    }

    public String getWsPublicUrl() {
        return wsPublicUrl;
    }

    public String getDataDir() {
        return dataDir;
    }

    public int getNumNodes() {
        return numNodes;
    }

    public TableDescription[] getTables() {
        return tables;
    }

    public int getNumSessionThreads() {
        return numSessionThreads;
    }

    public static String getPathname() {
        return pathname;
    }

    public int getBindPort() {
        return bindPort;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public String getBindIp() {
        return bindIp;
    }

    public int getSessionTimeoutMinutes() {
        return sessionTimeoutMinutes;
    }

    public Map<String, Object> getCustomData() {
        return customData;
    }

    public ClusterCfg createClusterConfig() {
        ClusterCfg cfg = new ClusterCfg();
        DataCfg datacfg = new DataCfg();
        datacfg.schema(tables);
        String dirs[] = new String[numNodes];
        for (int i = 0; i < dirs.length; i++) {
            dirs[i] = dataDir;
        }
        datacfg.dataDir(dirs);
        cfg.dataCluster(datacfg);
        return cfg;
    }

}
