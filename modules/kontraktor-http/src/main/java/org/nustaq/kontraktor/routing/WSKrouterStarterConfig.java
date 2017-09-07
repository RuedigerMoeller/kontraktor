package org.nustaq.kontraktor.routing;

import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kson.Kson;
import org.nustaq.utils.FileLookup;
import java.io.File;
import java.io.Serializable;

public class WSKrouterStarterConfig implements Serializable {

    public static WSKrouterStarterConfig read() {
        File lookup = new FileLookup("wskrouter.kson").lookup();
        Kson kson = new Kson()
            .map(RoutedServiceEntry.class)
            .map(WSKrouterStarterConfig.class);
        try {
            WSKrouterStarterConfig res = (WSKrouterStarterConfig) kson
                .readObject(lookup);
            return res;
        } catch (Exception e) {
            WSKrouterStarterConfig cfg = new WSKrouterStarterConfig();
            cfg.services = new RoutedServiceEntry[] { new RoutedServiceEntry("sample/v1/json", SerializerType.JsonNoRef) };
            try {
                String s = kson.writeObject(cfg);
                System.out.println("wskrouter.kson not found, defaulting to ");
                System.out.println(s);
                return cfg;
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return null;
    }

    String host = "localhost";
    int port = 6667;

    RoutedServiceEntry services[] = {};

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public RoutedServiceEntry[] getServices() {
        return services;
    }

    public WSKrouterStarterConfig host(String host) {
        this.host = host;
        return this;
    }

    public WSKrouterStarterConfig port(int port) {
        this.port = port;
        return this;
    }

    public WSKrouterStarterConfig services(RoutedServiceEntry[] services) {
        this.services = services;
        return this;
    }

    public static void main(String[] args) {
        read();
    }
}
