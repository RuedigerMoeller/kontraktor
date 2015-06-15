package org.nustaq.kontraktor.remoting.encoding;

import org.nustaq.serialization.FSTConfiguration;

import java.util.function.Consumer;

/**
 * Created by ruedi on 26.10.14.
 *
 * umh .. unfinished concept of custom serializers
 *
 */
public class Coding {
    SerializerType coding;
    Consumer<FSTConfiguration> configurator;

    public Coding(SerializerType coding) {
        this.coding = coding;
    }

    public Coding(SerializerType coding, Consumer<FSTConfiguration> configurator) {
        this.coding = coding;
        this.configurator = configurator;
    }

    public SerializerType getCoding() {
        return coding;
    }

    public Consumer<FSTConfiguration> getConfigurator() {
        return configurator;
    }

    public FSTConfiguration createConf() {
        FSTConfiguration conf;
        switch (coding) {
            case MinBin:
                conf = FSTConfiguration.createMinBinConfiguration();
                break;
            case Json:
                conf = FSTConfiguration.createJsonConfiguration();
                break;
            case JsonNoRef:
                conf = FSTConfiguration.createJsonConfiguration(false, false);
                break;
            case JsonNoRefPretty:
                conf = FSTConfiguration.createJsonConfiguration(true, false);
                break;
            case UnsafeBinary:
                conf = FSTConfiguration.createFastBinaryConfiguration();
                break;
            default:
                conf = FSTConfiguration.createDefaultConfiguration();
        }
        return conf;
    }

}
