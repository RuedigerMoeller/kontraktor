package de.ruedigermoeller.kontraktor;

/**
 * Created by moelrue on 20.05.2014.
 */
public interface Future<T> extends Callback<T> {
    public Future then( Callback<T> result );
    public <OUT> Future<OUT> filter(final Filter<T, OUT> filter);

    /**
     * @return result if already avaiable
     */
    public T getResult();
}
