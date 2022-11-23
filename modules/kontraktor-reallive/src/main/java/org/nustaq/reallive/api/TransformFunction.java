package org.nustaq.reallive.api;

public interface TransformFunction {
    /**
     *
     * @param key - the key inside outer record (if applicable, else null)
     * @param index - the index inside outer array (if applicable, else -1)
     * @param v
     * @return
     */
    Object apply(String key, int index, Object v );
}
