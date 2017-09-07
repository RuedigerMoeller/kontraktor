package org.nustaq.kontraktor.routing;

import org.nustaq.kontraktor.remoting.encoding.SerializerType;

import java.io.Serializable;

public class RoutedServiceEntry implements Serializable {

    String path;
    String encoding = SerializerType.JsonNoRef.toString();
    String strategy = FailoverStrategy.HotCold.toString();

    public RoutedServiceEntry(String path, SerializerType encoding) {
        this.path = path;
        this.encoding = encoding.toString();
    }

    public String getPath() {
        return path;
    }

    public SerializerType getEncoding() {
        return SerializerType.valueOf(encoding);
    }

    public FailoverStrategy getStrategy() {
        return FailoverStrategy.valueOf(strategy);
    }

}
