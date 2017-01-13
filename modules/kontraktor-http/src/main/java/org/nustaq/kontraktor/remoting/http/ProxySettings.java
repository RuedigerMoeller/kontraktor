package org.nustaq.kontraktor.remoting.http;

public class ProxySettings {

    private String proxy;
    private int proxyPort;
    private String proxyUser;
    private String proxyPassword;
    private static ProxySettings instance;
    
    public static synchronized ProxySettings getProxySettings() {
        return instance;
    }
    
    public static synchronized void setProxySettings(ProxySettings instance) {
        ProxySettings.instance = instance;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }
}
