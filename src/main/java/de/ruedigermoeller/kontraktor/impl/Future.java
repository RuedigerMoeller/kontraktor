package de.ruedigermoeller.kontraktor.impl;

import de.ruedigermoeller.kontraktor.Callback;

/**
 * Created by moelrue on 20.05.2014.
 */
public interface Future<T> extends Callback<T> {
    public void then( Callback<T> result );

    /**
     * @return result if already avaiable
     */
    public T getResult();
}
