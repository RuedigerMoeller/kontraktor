package org.nustaq.kontraktor.server;

import org.nustaq.kontraktor.wapi.UserConstraintRegistry;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ruedi on 23.03.17.
 */
public class TestUserConstraintRegistry implements UserConstraintRegistry {

    ConcurrentHashMap<String,String> storage = new ConcurrentHashMap<>();

    public TestUserConstraintRegistry() {
        try {
            String secret = UserConstraintRegistry.readSecret("/home/ruedi/projects/kontraktor/modules/kontraktor-wapi/src/main/script/secret.txt");
            storage.put("moru",secret);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void storeSecret(String ukey, String secr) {
        storage.put(ukey,secr);
    }

    @Override
    public String getSecret(String ukey) {
        return storage.get(ukey);
    }

    public static void main(String[] args) throws IOException {
        TestUserConstraintRegistry tr = new TestUserConstraintRegistry();
        String moru = tr.createSecret("moru");
//        PrintWriter pw = new PrintWriter(new FileWriter("/home/ruedi/projects/kontraktor/modules/kontraktor-wapi/src/main/script/secret.txt"));
//        pw.print(moru);
//        pw.close();
        String secret = UserConstraintRegistry.readSecret("/home/ruedi/projects/kontraktor/modules/kontraktor-wapi/src/main/script/secret.txt");
        System.out.println(secret);

    }
}
