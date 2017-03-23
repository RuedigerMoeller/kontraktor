package org.nustaq.kontraktor.server;

import org.nustaq.kontraktor.wapi.UserConstraintRegistry;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ruedi on 23.03.17.
 */
public class TestUserConstraintRegistry implements UserConstraintRegistry {

    ConcurrentHashMap<String,String> storage = new ConcurrentHashMap<>();

    @Override
    public void storeSecret(String ukey, String secr) {
        storage.put(ukey,secr);
    }

    @Override
    public String getSecret(String ukey) {
        return null;
    }

}
