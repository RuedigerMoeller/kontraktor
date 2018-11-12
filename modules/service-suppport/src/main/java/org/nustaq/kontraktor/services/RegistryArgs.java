package org.nustaq.kontraktor.services;

import com.beust.jcommander.Parameter;

public class RegistryArgs extends ServiceArgs {
    @Parameter(names = {"-dumpServices"}, help=true, description = "log services in console cyclically")
    private boolean dumpServices = false;

    public boolean dumpServices() {
        return dumpServices;
    }
}
