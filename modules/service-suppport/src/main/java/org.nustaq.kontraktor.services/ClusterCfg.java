package org.newstaq.kontraktor.services;

import org.newstaq.kontraktor.services.rlclient.DataCfg;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kson.Kson;
import org.nustaq.reallive.interfaces.TableDescription;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ruedi on 26.12.16.
 */
public class ClusterCfg {

    public static String pathname = "etc/cluster.kson";
    public static long lastTime;

    public static boolean isDirty() {
        return new File(pathname).lastModified() != lastTime;
    }

    public static ClusterCfg read() {
        lastTime = new File(pathname).lastModified();
        return read(pathname);
    }

    public static ClusterCfg read(String pathname) {
        Kson kson = new Kson().map( ClusterCfg.class, DataCfg.class, TableDescription.class );
        try {
            ClusterCfg clCfg = (ClusterCfg) kson.readObject(new File(pathname));
            String confString = kson.writeObject(clCfg);
            System.out.println("run with config from "+ new File(pathname).getCanonicalPath());
            System.out.println(confString);
            return clCfg;
        } catch (Exception e) {
            Log.Warn(null, pathname + " not found or parse error. " + e.getClass().getSimpleName() + ":" + e.getMessage());
            try {
                String sampleconf = kson.writeObject(new ClusterCfg());
                System.out.println("Defaulting to:\n"+sampleconf);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return new ClusterCfg();
    }

    String publicHostUrl = "http://localhost:8888";

    DataCfg dataCluster = new DataCfg();
    public DataCfg getDataCluster() {
        return dataCluster;
    }

    public ClusterCfg dataCluster(final DataCfg dataCluster) {
        this.dataCluster = dataCluster;
        return this;
    }


    public static void main(String[] args) {
        ClusterCfg read = read();
        System.out.println(read);
    }

}
