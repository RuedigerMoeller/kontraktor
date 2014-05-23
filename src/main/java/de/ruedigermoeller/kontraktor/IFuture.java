package de.ruedigermoeller.kontraktor;

/**
 * Created by moelrue on 20.05.2014.
 */
public interface IFuture<T> extends Callback<T> {
    public IFuture then( Callback<T> result );

    /**
     * @return result if already avaiable
     */
    public T getResult();
}
