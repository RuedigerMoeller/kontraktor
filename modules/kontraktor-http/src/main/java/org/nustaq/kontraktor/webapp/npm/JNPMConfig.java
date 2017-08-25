package org.nustaq.kontraktor.webapp.npm;

import org.nustaq.kontraktor.util.Log;
import org.nustaq.kson.Kson;
import org.nustaq.serialization.util.FSTUtil;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class JNPMConfig implements Serializable {

    Map<String,String> versionMap = new HashMap<>();

    public JNPMConfig() {
        versionMap.put("module-name","1.2.3");
    }

    public String getVersion(String moduleName) {
        return versionMap.get(moduleName);
    }

    public static JNPMConfig read() {
        return read("./run/etc/jnpm.kson");
    }

    public static JNPMConfig read(String pathname) {
        Kson kson = new Kson().map(JNPMConfig.class);
        try {
            JNPMConfig raw = (JNPMConfig) kson.readObject(new File(pathname));
            String confString = kson.writeObject(raw);
            System.out.println("JNPM run with config from "+ new File(pathname).getCanonicalPath());
            System.out.println(confString);
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
}
