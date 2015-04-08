package org.nustaq.kontraktor.remoting.spa;

import org.nustaq.kontraktor.annotations.Local;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by ruedi on 07/04/15.
 */
@Local
public class AppConf {

    public boolean devmode = true;
    public String componentPath[];
    public String rootComponent = "app";

    public int clientQSize = 1000;
    public String scan4Remote[];
    public boolean generateRealLiveSystemStubs = true;

    public HashMap<String,HashSet<String>> allowedMethods;
    public HashMap<String,HashSet<String>> forbiddenMethods;

    public List appSpecificConfig;

    public boolean isDevmode() {
        return devmode;
    }

    public AppConf setDevmode(boolean devmode) {
        this.devmode = devmode;
        return this;
    }

    public String[] getComponentPath() {
        return componentPath;
    }

    public AppConf setComponentPath(String[] componentPath) {
        this.componentPath = componentPath;
        return this;
    }

    public String getRootComponent() {
        return rootComponent;
    }

    public AppConf setRootComponent(String rootComponent) {
        this.rootComponent = rootComponent;
        return this;
    }

    public int getClientQSize() {
        return clientQSize;
    }

    public AppConf setClientQSize(int clientQSize) {
        this.clientQSize = clientQSize;
        return this;
    }

    public String[] getScan4Remote() {
        return scan4Remote;
    }

    public AppConf setScan4Remote(String[] scan4Remote) {
        this.scan4Remote = scan4Remote;
        return this;
    }

    public boolean isGenerateRealLiveSystemStubs() {
        return generateRealLiveSystemStubs;
    }

    public AppConf setGenerateRealLiveSystemStubs(boolean generateRealLiveSystemStubs) {
        this.generateRealLiveSystemStubs = generateRealLiveSystemStubs;
        return this;
    }

    public HashMap<String, HashSet<String>> getAllowedMethods() {
        return allowedMethods;
    }

    public AppConf setAllowedMethods(HashMap<String, HashSet<String>> allowedMethods) {
        this.allowedMethods = allowedMethods;
        return this;
    }

    public HashMap<String, HashSet<String>> getForbiddenMethods() {
        return forbiddenMethods;
    }

    public AppConf setForbiddenMethods(HashMap<String, HashSet<String>> forbiddenMethods) {
        this.forbiddenMethods = forbiddenMethods;
        return this;
    }

    public List getAppSpecificConfig() {
        return appSpecificConfig;
    }

    public AppConf setAppSpecificConfig(List appSpecificConfig) {
        this.appSpecificConfig = appSpecificConfig;
        return this;
    }
}
