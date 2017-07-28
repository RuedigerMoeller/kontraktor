package org.nustaq.kontraktor.weblication;

import org.nustaq.kontraktor.util.Log;
import org.nustaq.kson.Kson;
import org.nustaq.reallive.api.Record;

import java.io.File;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Created by ruedi on 20.06.17.
 */
public class BasicWebAppConfig implements Serializable {

    public static BasicWebAppConfig read(Class<? extends BasicWebAppConfig> target,Class ... mapped) {
        return read("./run/etc/app.kson", mapped,target);
    }

    public static BasicWebAppConfig read() {
        return read(BasicWebAppConfig.class);
    }

    public static BasicWebAppConfig read(String pathname, Class[] mappedClasses, Class<? extends BasicWebAppConfig> target) {
        Kson kson = new Kson().map(target);
        if ( mappedClasses != null ) {
            kson.map(mappedClasses);
        }
        try {
            Object raw = kson.readObject(new File(pathname));
            BasicWebAppConfig cfg = (BasicWebAppConfig) raw;
            String confString = kson.writeObject(cfg);
            System.out.println("run with config from "+ new File(pathname).getCanonicalPath());
            System.out.println(confString);
            return cfg;
        } catch (Exception e) {
            Log.Warn(null, pathname + " not found or parse error. " + e.getClass().getSimpleName() + ":" + e.getMessage());
            try {
                String sampleconf = kson.writeObject(target.newInstance());
                System.out.println("Defaulting to:\n"+sampleconf);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return new BasicWebAppConfig();
    }


    protected String staticUrlPrefix="/filez/";
    protected String staticFileRoot="./run/filez";
    protected int numSessionThreads = 4;
    protected boolean dev=true;
    protected int port = 8080;
    protected String host = "localhost";
    protected String clientRoot = "./src/main/web/client";
    protected long sessionTimeoutMS = TimeUnit.MINUTES.toMillis(5);

    public int getNumSessionThreads() {
        return numSessionThreads;
    }

    public BasicWebAppConfig numSessionThreads(int numSessionThreads) {
        this.numSessionThreads = numSessionThreads;
        return this;
    }

    public boolean getDev() {
        return dev;
    }

    public Class[] getMessageClasses() {
        return new Class[] {
            BasicAuthenticationResult.class,
            Record.class
        };
    }

    public BasicWebAppConfig dev(boolean dev) {
        this.dev = dev;
        return this;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public BasicWebAppConfig port(int port) {
        this.port = port;
        return this;
    }

    public BasicWebAppConfig host(String host) {
        this.host = host;
        return this;
    }

    public String getClientRoot() {
        return clientRoot;
    }

    public long getSessionTimeoutMS() {
        return sessionTimeoutMS;
    }

    public BasicWebAppConfig clientRoot(String clientRoot) {
        this.clientRoot = clientRoot;
        return this;
    }

    public BasicWebAppConfig sessionTimeoutMS(long sessionTimeoutMS) {
        this.sessionTimeoutMS = sessionTimeoutMS;
        return this;
    }

    public String getStaticUrlPrefix() {
        return staticUrlPrefix;
    }

    public String getStaticFileRoot() {
        return staticFileRoot;
    }

    public BasicWebAppConfig staticUrlPrefix(String staticUrlPrefix) {
        this.staticUrlPrefix = staticUrlPrefix;
        return this;
    }

    public BasicWebAppConfig staticFileRoot(String staticFileRoot) {
        this.staticFileRoot = staticFileRoot;
        return this;
    }
}
