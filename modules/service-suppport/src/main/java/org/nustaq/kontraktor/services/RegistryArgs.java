package org.nustaq.kontraktor.services;

import com.beust.jcommander.Parameter;

public class RegistryArgs extends ServiceArgs {
    @Parameter(names = {"-logServices"}, help=true, description = "log services in console cyclically")
    private boolean logServices = false;

    public boolean isLogServices() {
        return logServices;
    }
}
