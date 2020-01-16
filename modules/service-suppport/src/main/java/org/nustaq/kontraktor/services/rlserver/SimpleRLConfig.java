package org.nustaq.kontraktor.services.rlserver;

import org.nustaq.kontraktor.util.Log;
import org.nustaq.kson.Kson;
import org.nustaq.reallive.api.TableDescription;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

public class SimpleRLConfig implements Serializable {

    public static SimpleRLConfig get;


    String dataDir = "./data";
    int numNodes = 3;
    TableDescription tables[];
    int numSessionThreads = 4;
    int bindPort = 8081;
    boolean runDataClusterInsideWebserver = false;

    // API endpoint
    String publicUrl = "http://localhost:"+bindPort;
    String bindIp = "0.0.0.0";
    String wsPublicUrl = "ws://localhost:"+bindPort+"/ws";

    Map<String,Object> customData;

    int sessionTimeoutMinutes = 10;

    public static String pathname = "./etc/reallive.kson";

    public static SimpleRLConfig read() {
        return read(pathname);
    }

    public static SimpleRLConfig read(String pathname) {
        Kson kson = new Kson().map( SimpleRLConfig.class, TableDescription.class );
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
}
