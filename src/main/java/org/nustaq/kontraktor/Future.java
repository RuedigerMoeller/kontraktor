package org.nustaq.kontraktor;

/**
 * Created by moelrue on 20.05.2014.
 */
public interface Future<T> extends Callback<T> {
    public Future<T> then( Runnable result );
    public Future<T> then( Callback<T> result );
    public <OUT> Future<OUT> map(final Filter<T, OUT> filter);

    /**
     * @return result if already avaiable
     */
    public T getResult();
    public Object getError();
    /**
     * same as receive(null,null)
     */
    public void signal();
}
