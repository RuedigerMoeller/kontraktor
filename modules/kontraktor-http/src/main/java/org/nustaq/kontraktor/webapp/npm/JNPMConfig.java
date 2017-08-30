package org.nustaq.kontraktor.webapp.npm;

import org.nustaq.kontraktor.util.Log;
import org.nustaq.kson.Kson;
import org.nustaq.serialization.util.FSTUtil;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class JNPMConfig implements Serializable {

    // overide version of a node module
    protected Map<String,String> versionMap = new HashMap<>();
    // replace module with another module. This is applied directly to the require(..) parameter string
    protected Map<String,String> nodeLibraryMap = new HashMap<>();
    protected String repo = "http://registry.npmjs.org/";
    protected String transformFunction = "React.createElement";

    public JNPMConfig() {
        versionMap.put("module-name","^1.2.3");
    }

    public String getVersion(String moduleName) {
        return versionMap.get(moduleName);
    }

    public void putVersion( String moduleName, String spec ) {
        versionMap.put(moduleName,spec);
    }

    public String getTransformFunction() {
        return transformFunction;
    }

    public Map<String, String> getVersionMap() {
        return versionMap;
    }

    public Map<String, String> getNodeLibraryMap() {
        return nodeLibraryMap;
    }

    public String getRepo() {
        return repo;
    }

    public static JNPMConfig read() {
        return read("./run/etc/jnpm.kson");
    }

    public static JNPMConfig read(String pathname) {
        Kson kson = new Kson().map(JNPMConfig.class);
        try {
            JNPMConfig raw = (JNPMConfig) kson.readObject(new File(pathname));
//            String confString = kson.writeObject(raw);
//            System.out.println("JNPM run with config from "+ new File(pathname).getCanonicalPath());
//            System.out.println(confString);
            return raw;
        } catch (Exception e) {
            Log.Warn(null, pathname + " not found or parse error. " + e.getClass().getSimpleName() + ":" + e.getMessage());
            try {
                String sampleconf = kson.writeObject(new JNPMConfig());
                System.out.println("JNPM Defaulting to:\n"+sampleconf);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        try {
            return new JNPMConfig();
        } catch (Exception e) {
            FSTUtil.rethrow(e);
        }
        return null;
    }

    public static void main(String[] args) {
        read();
    }

    public JNPMConfig versionMap(Map<String, String> versionMap) {
        this.versionMap = versionMap;
        return this;
    }

    public JNPMConfig repo(String repo) {
        this.repo = repo;
        return this;
    }

    public JNPMConfig transformFunction(String transFormFunction) {
        this.transformFunction = transFormFunction;
        return this;
    }
}
