package org.nustaq.kontraktor.services;

import org.nustaq.kontraktor.services.rlclient.DataCfg;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kson.Kson;
import org.nustaq.reallive.api.TableDescription;

import java.io.File;
import java.io.Serializable;

/**
 * Created by ruedi on 26.12.16.
 */
public class ClusterCfg implements Serializable {

    public static String pathname = "run/etc/clustercfg.kson";
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
//            System.out.println(confString);
            return clCfg;
        } catch (Exception e) {
            Log.Warn(null, pathname + " not found or parse error. " + e.getClass().getSimpleName() + ":" + e.getMessage());
            try {
                String sampleconf = kson.writeObject(new ClusterCfg());
                System.out.println("Try:\n"+sampleconf);
                System.exit(1);
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
