package org.nustaq.reallive.api;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class KeySetSubscriber extends Subscriber {

    public static interface KSPredicate<T> extends RLPredicate<T> {
        Set<String> getKeys();
    }

    private final Set<String> keys;

    public KeySetSubscriber(String[] keys, ChangeReceiver receiver) {
        super(null, receiver);
        this.keys = new HashSet();
        this.keys.addAll(Arrays.asList(keys));
        Set<String> finKeys = this.keys;
        filter = new KSPredicate() {

            @Override
            public boolean test(Object o) {
                return false;
            }

            @Override
            public Set<String> getKeys() {
                return finKeys;
            }
        };
    }
}
