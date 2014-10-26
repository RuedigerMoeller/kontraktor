package org.nustaq.kontraktor.remoting;

import org.nustaq.serialization.FSTConfiguration;

import java.util.function.Consumer;

/**
 * Created by ruedi on 26.10.14.
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
}
