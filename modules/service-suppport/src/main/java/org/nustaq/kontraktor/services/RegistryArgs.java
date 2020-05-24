package org.nustaq.kontraktor.services;

import com.beust.jcommander.Parameter;

import java.util.function.Supplier;

public class RegistryArgs extends ServiceArgs {

    public static Supplier<RegistryArgs> factory = () -> new RegistryArgs();
    public static RegistryArgs New() {
        return factory.get();
    }


    @Parameter(names = {"-dumpServices"}, help=true, description = "log services in console cyclically")
    private boolean dumpServices = false;

    protected RegistryArgs() {
        super();
    }

    public boolean dumpServices() {
        return dumpServices;
    }
}
